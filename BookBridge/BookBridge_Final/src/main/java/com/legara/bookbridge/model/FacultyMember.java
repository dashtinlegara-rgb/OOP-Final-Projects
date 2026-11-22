package com.legara.bookbridge.model;

public class FacultyMember extends Member {
    public FacultyMember(String memberId, String memberName) { super(memberId, memberName); }
    @Override public int getMaximumLoanDays() { return 30; }
    @Override public MembershipType getMembershipType() { return MembershipType.FACULTY; }
    @Override public int getLoanLimit() { return 5; }
    @Override public double getDailyFine() { return 2.0; }
}
