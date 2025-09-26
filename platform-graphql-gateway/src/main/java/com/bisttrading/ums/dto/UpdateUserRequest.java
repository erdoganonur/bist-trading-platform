package com.bisttrading.ums.dto;

public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String language;

    public UpdateUserRequest() {}

    public UpdateUserRequest(String firstName, String lastName, String phoneNumber, String language) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.language = language;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}