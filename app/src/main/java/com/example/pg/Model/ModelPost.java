package com.example.pg.Model;

public class ModelPost {


    String pId, pTitle , pDrscr, pLike, pComments,pImage , pTime, uid ,uUsername, uDp , uName ;

    public ModelPost() {
    }

    public ModelPost(String pId, String pTitle, String pDrscr, String pLike, String pComments, String pImage, String pTime, String uid, String uUsername, String uDp, String uName) {
        this.pId = pId;
        this.pTitle = pTitle;
        this.pDrscr = pDrscr;
        this.pLike = pLike;
        this.pComments = pComments;
        this.pImage = pImage;
        this.pTime = pTime;
        this.uid = uid;
        this.uUsername = uUsername;
        this.uDp = uDp;
        this.uName = uName;
    }

    public String getpId() {
        return pId;
    }

    public void setpId(String pId) {
        this.pId = pId;
    }

    public String getpTitle() {
        return pTitle;
    }

    public void setpTitle(String pTitle) {
        this.pTitle = pTitle;
    }

    public String getpDrscr() {
        return pDrscr;
    }

    public void setpDrscr(String pDrscr) {
        this.pDrscr = pDrscr;
    }

    public String getpLike() {
        return pLike;
    }

    public void setpLike(String pLike) {
        this.pLike = pLike;
    }

    public String getpComments() {
        return pComments;
    }

    public void setpComments(String pComments) {
        this.pComments = pComments;
    }

    public String getpImage() {
        return pImage;
    }

    public void setpImage(String pImage) {
        this.pImage = pImage;
    }

    public String getpTime() {
        return pTime;
    }

    public void setpTime(String pTime) {
        this.pTime = pTime;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
    public void setuUsername(String uUsername) {
        this.uUsername = uUsername;
    }

    public String getuUsername() {
        return this.uUsername;
    }



    public String getuDp() {
        return uDp;
    }

    public void setuDp(String uDp) {
        this.uDp = uDp;
    }

    public String getuName() {
        return uName;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }
}
