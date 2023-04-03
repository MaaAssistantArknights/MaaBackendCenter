package plus.maa.backend.service.session;

import plus.maa.backend.repository.entity.MaaUser;

import java.util.Set;


public class UserSession {
    private MaaUser maaUser;
    private String token = "";
    private Set<String> permissions;

    public MaaUser getMaaUser() {
        return maaUser;
    }

    public void setMaaUser(MaaUser maaUser) {
        this.maaUser = maaUser;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
