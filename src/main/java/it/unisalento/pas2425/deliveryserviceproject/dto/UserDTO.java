package it.unisalento.pas2425.deliveryserviceproject.dto;

import it.unisalento.pas2425.deliveryserviceproject.domain.GenderType;
import it.unisalento.pas2425.deliveryserviceproject.domain.Role;
import it.unisalento.pas2425.deliveryserviceproject.domain.UserStatus;

public class UserDTO {

    private String id;
    private String name;
    private String surname;
    private String email;
    private String password;
    private Role role;
    private GenderType genderType;
    private String phoneNumber;
    private String resetCode;
    private UserStatus userStatus;
    private String pushToken;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public Role getRole() {
        return role;
    }

    public GenderType getGenderType() {
        return genderType;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setGenderType(GenderType genderType) {
        this.genderType = genderType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {      //in teoria non dovremmo usarlo per passare la pwd cifrata al client
        this.password = password;
    }


    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getResetCode() {
        return resetCode;
    }

    public void setResetCode(String resetCode) {
        this.resetCode = resetCode;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }
}
