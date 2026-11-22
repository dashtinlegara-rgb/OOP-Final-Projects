package com.legara.bookbridge.model;

public abstract class Member {
    private final String memberId;
    private final String memberName;

    public Member(String memberId, String memberName) {
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new IllegalArgumentException("Member ID cannot be blank.");
        }
        if (memberName == null || memberName.trim().isEmpty()) {
            throw new IllegalArgumentException("Member name cannot be blank.");
        }
        this.memberId = memberId.trim();
        this.memberName = memberName.trim();
    }

    public static Member create(MembershipType type, String memberId, String memberName) {
        return switch (type) {
            case STUDENT -> new StudentMember(memberId, memberName);
            case FACULTY -> new FacultyMember(memberId, memberName);
            case GUEST -> new GuestMember(memberId, memberName);
        };
    }

    public String getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }

    public abstract int getMaximumLoanDays();
    public abstract double getDailyFine();
    public abstract MembershipType getMembershipType();
    public abstract int getLoanLimit();
}
