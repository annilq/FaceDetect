package com.yunqi.hospital.device;

/**
 * 社保卡
 */
public class HealthCard {
    private String name; // 姓名
    private String cardNo; // 社保卡号
    private String sex; // 性别
    private String idCardNo; // 身份证号
    private String districtCode; // 区域码
    private String cardVersion; // 规范版本
    private String cardSN; // 卡序列号

    public HealthCard(String name, String cardNo, String sex, String idCardNo, String districtCode, String cardVersion, String cardSN) {
        this.name = name;
        this.cardNo = cardNo;
        this.sex = sex;
        this.idCardNo = idCardNo;
        this.districtCode = districtCode;
        this.cardVersion = cardVersion;
        this.cardSN = cardSN;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getIdCardNo() {
        return idCardNo;
    }

    public void setIdCardNo(String idCardNo) {
        this.idCardNo = idCardNo;
    }

    public String getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(String districtCode) {
        this.districtCode = districtCode;
    }

    public String getCardVersion() {
        return cardVersion;
    }

    public void setCardVersion(String cardVersion) {
        this.cardVersion = cardVersion;
    }

    public String getCardSN() {
        return cardSN;
    }

    public void setCardSN(String cardSN) {
        this.cardSN = cardSN;
    }
}
