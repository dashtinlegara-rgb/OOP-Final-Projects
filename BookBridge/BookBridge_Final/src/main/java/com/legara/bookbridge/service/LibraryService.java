package com.legara.bookbridge.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import com.legara.bookbridge.exception.DataAccessException;
import com.legara.bookbridge.exception.DuplicateEntityException;
import com.legara.bookbridge.exception.EntityNotFoundException;
import com.legara.bookbridge.exception.InvalidOperationException;
import com.legara.bookbridge.exception.ItemUnavailableException;
import com.legara.bookbridge.exception.LoanLimitExceededException;
import com.legara.bookbridge.model.BookStatus;
import com.legara.bookbridge.model.LibraryBook;
import com.legara.bookbridge.model.LoanRecord;
import com.legara.bookbridge.model.LoanStatus;
import com.legara.bookbridge.model.Member;
import com.legara.bookbridge.model.MembershipType;
import com.legara.bookbridge.model.Reservation;
import com.legara.bookbridge.model.ReservationStatus;

public class LibraryService {
    private final Repository<Member, String> members = new Repository<>("Member", "ID", Member::getMemberId);
    private final Repository<LibraryBook, String> catalog = new Repository<>("Book", "ISBN", LibraryBook::getIsbn);

    private final Map<String, LoanRecord> activeLoans = new LinkedHashMap<>();
    private final Set<String> checkedOutIsbns = new HashSet<>();
    private final List<LoanRecord> loanHistory = new ArrayList<>();

    private final Queue<Reservation> pendingReservations = new LinkedList<>();
    private final Map<String, Reservation> reservations = new LinkedHashMap<>();

    private final LibraryStore store;
    private LocalDate fixedToday;

    public LibraryService(LibraryStore store) {
        this.store = Objects.requireNonNull(store, "Store is required.");
    }

    public synchronized LocalDate today() {
        return fixedToday != null ? fixedToday : LocalDate.now();
    }

    public synchronized void setToday(LocalDate today) {
        this.fixedToday = today;
    }

    public synchronized void loadFromStore() throws DataAccessException {
        List<Member> loadedMembers = store.loadMembers();
        List<LibraryBook> loadedBooks = store.loadBooks();
        Map<String, Member> memberById = new HashMap<>();
        for (Member m : loadedMembers) {
            memberById.put(m.getMemberId(), m);
        }
        Map<String, LibraryBook> bookByIsbn = new HashMap<>();
        for (LibraryBook b : loadedBooks) {
            bookByIsbn.put(b.getIsbn(), b);
        }
        List<LoanRecord> loadedLoans = store.loadLoans(memberById, bookByIsbn);
        List<Reservation> loadedReservations = store.loadReservations(memberById, bookByIsbn);

        members.replaceAll(loadedMembers);
        catalog.replaceAll(loadedBooks);
        activeLoans.clear();
        checkedOutIsbns.clear();
        loanHistory.clear();
        pendingReservations.clear();
        reservations.clear();

        for (LoanRecord loan : loadedLoans) {
            loanHistory.add(loan);
            if (loan.isActive()) {
                activeLoans.put(loan.getLoanId(), loan);
                checkedOutIsbns.add(loan.getBook().getIsbn());
                loan.getBook().markBorrowed();
            }
        }
        for (Reservation r : loadedReservations) {
            reservations.put(r.getReservationId(), r);
            if (r.isPending()) {
                pendingReservations.add(r);
            }
        }
    }

    public synchronized void registerMember(Member member) throws DuplicateEntityException, DataAccessException {
        Objects.requireNonNull(member, "Member is required.");
        members.ensureAbsent(member);
        store.insertMember(member);
        members.add(member);
    }

    public synchronized Member getMember(String memberId) throws EntityNotFoundException {
        return members.require(memberId);
    }

    public synchronized List<Member> listMembers() {
        return members.list();
    }

    public synchronized void addBook(LibraryBook book) throws DuplicateEntityException, DataAccessException {
        Objects.requireNonNull(book, "Book is required.");
        catalog.ensureAbsent(book);
        store.insertBook(book);
        catalog.add(book);
    }

    public synchronized LibraryBook getBook(String isbn) throws EntityNotFoundException {
        return catalog.require(isbn);
    }

    public synchronized List<LibraryBook> listBooks() {
        return catalog.list();
    }

    public synchronized LoanRecord issueLoan(String memberId, String isbn, int loanDays)
            throws EntityNotFoundException, ItemUnavailableException, LoanLimitExceededException, DataAccessException {
        Member member = getMember(memberId);
        LibraryBook book = getBook(isbn);
        checkCanBorrow(member, book, loanDays);

        Reservation first = firstPendingFor(book.getIsbn());
        Reservation fulfilled = (first != null && first.getMember() == member) ? first : null;
        return commitLoan(member, book, loanDays, fulfilled);
    }

    public synchronized LoanRecord returnBook(String loanId) throws EntityNotFoundException, DataAccessException {
        LoanRecord loan = requireActiveLoan(loanId);
        LocalDate today = today();
        store.markLoanReturned(loanId, today);
        loan.markReturned(today);
        activeLoans.remove(loanId);
        checkedOutIsbns.remove(loan.getBook().getIsbn());
        loan.getBook().markReturned();
        return loan;
    }

    public synchronized LoanRecord renewLoan(String loanId, int additionalDays)
            throws EntityNotFoundException, InvalidOperationException, LoanLimitExceededException, DataAccessException {
        LoanRecord loan = requireActiveLoan(loanId);
        if (additionalDays < 1) {
            throw new InvalidOperationException("A renewal must add at least 1 day.");
        }
        if (loan.isOverdue(today())) {
            throw new InvalidOperationException("Loan " + loanId + " is overdue (due " + loan.getDueDate()
                    + "); return it and settle the fine instead of renewing.");
        }
        if (firstPendingFor(loan.getBook().getIsbn()) != null) {
            throw new InvalidOperationException("'" + loan.getBook().getTitle()
                    + "' is reserved by another member, so the loan cannot be renewed.");
        }
        if (!loan.canRenew(additionalDays, today())) {
            throw new LoanLimitExceededException("Renewal would exceed the " + loan.getMember().getMaximumLoanDays()
                    + "-day maximum for " + loan.getMember().getMembershipType() + ".");
        }
        store.updateDueDate(loanId, loan.getDueDate().plusDays(additionalDays));
        loan.renew(additionalDays, today());
        return loan;
    }

    public synchronized int countActiveLoans(String memberId) {
        int count = 0;
        for (LoanRecord loan : activeLoans.values()) {
            if (loan.getMember().getMemberId().equals(memberId)) {
                count++;
            }
        }
        return count;
    }

    public synchronized boolean isCheckedOut(String isbn) {
        return checkedOutIsbns.contains(isbn);
    }

    public synchronized List<LoanRecord> listActiveLoans() {
        return List.copyOf(activeLoans.values());
    }

    public synchronized List<LoanRecord> getLoanHistory() {
        return List.copyOf(loanHistory);
    }

    public synchronized Reservation reserveBook(String memberId, String isbn)
            throws EntityNotFoundException, InvalidOperationException, DuplicateEntityException, DataAccessException {
        Member member = getMember(memberId);
        LibraryBook book = getBook(isbn);
        if (!checkedOutIsbns.contains(book.getIsbn())) {
            throw new InvalidOperationException("'" + book.getTitle() + "' is on the shelf; borrow it instead of reserving.");
        }
        for (LoanRecord loan : activeLoans.values()) {
            if (loan.getBook() == book && loan.getMember() == member) {
                throw new InvalidOperationException(member.getMemberName() + " already has '" + book.getTitle() + "' on loan.");
            }
        }
        for (Reservation r : pendingReservations) {
            if (r.getBook() == book && r.getMember() == member) {
                throw new DuplicateEntityException(member.getMemberName() + " already has a pending reservation ("
                        + r.getReservationId() + ") for '" + book.getTitle() + "'.");
            }
        }
        Reservation reservation = new Reservation(member, book, today());
        store.insertReservation(reservation);
        pendingReservations.add(reservation);
        reservations.put(reservation.getReservationId(), reservation);
        return reservation;
    }

    public synchronized Reservation cancelReservation(String reservationId)
            throws EntityNotFoundException, InvalidOperationException, DataAccessException {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            throw new EntityNotFoundException("No reservation with ID: " + reservationId);
        }
        if (!reservation.isPending()) {
            throw new InvalidOperationException("Reservation " + reservationId + " is already " + reservation.getStatus() + ".");
        }
        store.updateReservationStatus(reservationId, ReservationStatus.CANCELLED);
        pendingReservations.remove(reservation);
        reservation.markCancelled();
        return reservation;
    }

    public synchronized LoanRecord processNextReservation()
            throws InvalidOperationException, ItemUnavailableException, LoanLimitExceededException, DataAccessException {
        if (pendingReservations.isEmpty()) {
            throw new InvalidOperationException("No pending reservations to process.");
        }
        for (Reservation r : pendingReservations) {
            if (!checkedOutIsbns.contains(r.getBook().getIsbn())) {
                Member member = r.getMember();
                int days = member.getMaximumLoanDays();
                checkCanBorrow(member, r.getBook(), days);
                return commitLoan(member, r.getBook(), days, r);
            }
        }
        throw new ItemUnavailableException("All " + pendingReservations.size()
                + " pending reservation(s) are waiting for books that are still checked out.");
    }

    public synchronized List<Reservation> listPendingReservations() {
        return List.copyOf(pendingReservations);
    }

    public synchronized List<LoanRecord> listActiveLoansSorted() {
        LocalDate today = today();
        Comparator<LoanRecord> order = Comparator
                .comparing((LoanRecord loan) -> !loan.isOverdue(today))
                .thenComparing(LoanRecord::getDueDate)
                .thenComparing(LoanRecord::getLoanId);
        List<LoanRecord> sorted = new ArrayList<>(activeLoans.values());
        sorted.sort(order);
        return List.copyOf(sorted);
    }

    public synchronized List<LibraryBook> searchBooksByTitle(String keyword) {
        String needle = keyword == null ? "" : keyword.trim().toLowerCase();
        return catalog.listWhere(book -> book.getTitle().toLowerCase().contains(needle));
    }

    public synchronized List<LibraryBook> filterBooksByStatus(BookStatus status) {
        return catalog.listWhere(book -> book.getStatus() == status);
    }

    public synchronized List<Member> filterMembersByType(MembershipType type) {
        return members.listWhere(member -> member.getMembershipType() == type);
    }

    public synchronized List<LoanRecord> loansForMember(String memberId) {
        List<LoanRecord> result = new ArrayList<>();
        for (LoanRecord loan : loanHistory) {
            if (loan.getMember().getMemberId().equals(memberId)) {
                result.add(loan);
            }
        }
        return result;
    }

    public synchronized List<LoanRecord> filterLoansByStatus(LoanStatus status) {
        List<LoanRecord> result = new ArrayList<>();
        for (LoanRecord loan : loanHistory) {
            if (loan.getStatus() == status) {
                result.add(loan);
            }
        }
        return result;
    }

    public synchronized CirculationSummary summary() {
        LocalDate today = today();
        int available = 0;
        for (LibraryBook book : catalog.list()) {
            if (book.isAvailable()) {
                available++;
            }
        }
        Map<MembershipType, Integer> perType = new HashMap<>();
        for (MembershipType type : MembershipType.values()) {
            perType.put(type, 0);
        }
        int overdue = 0;
        for (LoanRecord loan : activeLoans.values()) {
            MembershipType type = loan.getMember().getMembershipType();
            perType.put(type, perType.get(type) + 1);
            if (loan.isOverdue(today)) {
                overdue++;
            }
        }
        double fines = 0;
        for (LoanRecord loan : loanHistory) {
            fines += loan.estimatedFine(today);
        }
        return new CirculationSummary(available, checkedOutIsbns.size(), Map.copyOf(perType), fines, overdue,
                pendingReservations.size(), loanHistory.size());
    }

    public synchronized LibrarySnapshot snapshot() {
        return new LibrarySnapshot(today(), listBooks(), listMembers(), listActiveLoansSorted(),
                listPendingReservations(), summary());
    }

    private void checkCanBorrow(Member member, LibraryBook book, int loanDays)
            throws ItemUnavailableException, LoanLimitExceededException {
        if (checkedOutIsbns.contains(book.getIsbn())) {
            throw new ItemUnavailableException("'" + book.getTitle() + "' (ISBN " + book.getIsbn() + ") is already checked out.");
        }
        Reservation first = firstPendingFor(book.getIsbn());
        if (first != null && first.getMember() != member) {
            throw new ItemUnavailableException("'" + book.getTitle() + "' is on hold for " + first.getMember().getMemberName()
                    + " (reservation " + first.getReservationId() + ").");
        }
        int activeCount = countActiveLoans(member.getMemberId());
        if (activeCount >= member.getLoanLimit()) {
            throw new LoanLimitExceededException(member.getMemberName() + " already holds " + activeCount
                    + " active loan(s); the " + member.getMembershipType() + " limit is " + member.getLoanLimit() + ".");
        }
        if (loanDays < 1 || loanDays > member.getMaximumLoanDays()) {
            throw new LoanLimitExceededException("Loan length must be 1 to " + member.getMaximumLoanDays()
                    + " days for " + member.getMembershipType() + "; requested " + loanDays + ".");
        }
    }

    private LoanRecord commitLoan(Member member, LibraryBook book, int loanDays, Reservation fulfilled)
            throws DataAccessException {
        LoanRecord loan = new LoanRecord(member, book, today(), loanDays);
        store.insertLoan(loan, fulfilled == null ? null : fulfilled.getReservationId());
        activeLoans.put(loan.getLoanId(), loan);
        checkedOutIsbns.add(book.getIsbn());
        loanHistory.add(loan);
        book.markBorrowed();
        if (fulfilled != null) {
            pendingReservations.remove(fulfilled);
            fulfilled.markFulfilled();
        }
        return loan;
    }

    private LoanRecord requireActiveLoan(String loanId) throws EntityNotFoundException {
        LoanRecord loan = activeLoans.get(loanId);
        if (loan == null) {
            throw new EntityNotFoundException("No active loan with ID: " + loanId);
        }
        return loan;
    }

    private Reservation firstPendingFor(String isbn) {
        for (Reservation r : pendingReservations) {
            if (r.getBook().getIsbn().equals(isbn)) {
                return r;
            }
        }
        return null;
    }
}
