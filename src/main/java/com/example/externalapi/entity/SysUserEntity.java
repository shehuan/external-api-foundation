package com.example.externalapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户信息。
 * </p>
 *
 * @author codex
 * @since 2026-05-07
 */
@TableName("sys_user")
public class SysUserEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID。
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 部门 ID。
     */
    @TableField("dept_id")
    private Long deptId;

    /**
     * 用户账号。
     */
    @TableField("user_name")
    private String userName;

    /**
     * 用户昵称。
     */
    @TableField("nick_name")
    private String nickName;

    /**
     * 用户类型。00 表示系统用户。
     */
    @TableField("user_type")
    private String userType;

    /**
     * 用户邮箱。
     */
    @TableField("email")
    private String email;

    /**
     * 手机号码。
     */
    @TableField("phonenumber")
    private String phonenumber;

    /**
     * 用户性别。0 男，1 女，2 未知。
     */
    @TableField("sex")
    private Integer sex;

    /**
     * 头像地址。
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 密码摘要。
     */
    @TableField("password")
    private String password;

    /**
     * 账号状态。0 正常，1 停用。
     */
    @TableField("status")
    private Integer status;

    /**
     * 最后登录 IP。
     */
    @TableField("login_ip")
    private String loginIp;

    /**
     * 最后登录时间。
     */
    @TableField("login_date")
    private LocalDateTime loginDate;

    /**
     * 备注。
     */
    @TableField("remark")
    private String remark;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    public LocalDateTime getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(LocalDateTime loginDate) {
        this.loginDate = loginDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "SysUserEntity{" +
            "userId = " + userId +
            ", deptId = " + deptId +
            ", userName = " + userName +
            ", nickName = " + nickName +
            ", userType = " + userType +
            ", email = " + email +
            ", phonenumber = " + phonenumber +
            ", sex = " + sex +
            ", avatar = " + avatar +
            ", password = " + password +
            ", status = " + status +
            ", loginIp = " + loginIp +
            ", loginDate = " + loginDate +
            ", remark = " + remark +
            "}";
    }
}
