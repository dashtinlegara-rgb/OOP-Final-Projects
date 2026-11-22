package com.legara.bookbridge.model;

public class GuestMember extends Member {
    public GuestMember(String memberId, String memberName) { super(memberId, memberName); }
    @Override public int getMaximumLoanDays() { return 7; }
    @Override public MembershipType getMembershipType() { return MembershipType.GUEST; }
    @Override public int getLoanLimit() { return 1; }
    @Override public double getDailyFine() { return 10.0; }
}
