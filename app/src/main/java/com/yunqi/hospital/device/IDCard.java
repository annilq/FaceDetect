package com.yunqi.hospital.device;

/**
 * 身份证
 */
public class IDCard {
    private String name; // 姓名
    private String idNumber; // 身份证号
    private String sex; // 性别
    private String address; // 地址
    private String birthDay; // 出生年月日

    public IDCard(String name, String idNumber, String sex, String address, String birthDay) {
        this.name = name;
        this.idNumber = idNumber;
        this.sex = sex;
        this.address = address;
        this.birthDay = birthDay;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(String birthDay) {
        this.birthDay = birthDay;
    }
}
