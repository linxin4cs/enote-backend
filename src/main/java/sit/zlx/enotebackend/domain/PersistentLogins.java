package sit.zlx.enotebackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName persistent_logins
 */
@TableName(value ="persistent_logins")
@Data
public class PersistentLogins implements Serializable {
    /**
     * 
     */
    @TableId(value = "series")
    private String series;

    /**
     * 
     */
    @TableField(value = "username")
    private String username;

    /**
     * 
     */
    @TableField(value = "token")
    private String token;

    /**
     * 
     */
    @TableField(value = "last_used")
    private Date last_used;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        PersistentLogins other = (PersistentLogins) that;
        return (this.getSeries() == null ? other.getSeries() == null : this.getSeries().equals(other.getSeries()))
            && (this.getUsername() == null ? other.getUsername() == null : this.getUsername().equals(other.getUsername()))
            && (this.getToken() == null ? other.getToken() == null : this.getToken().equals(other.getToken()))
            && (this.getLast_used() == null ? other.getLast_used() == null : this.getLast_used().equals(other.getLast_used()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getSeries() == null) ? 0 : getSeries().hashCode());
        result = prime * result + ((getUsername() == null) ? 0 : getUsername().hashCode());
        result = prime * result + ((getToken() == null) ? 0 : getToken().hashCode());
        result = prime * result + ((getLast_used() == null) ? 0 : getLast_used().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", series=").append(series);
        sb.append(", username=").append(username);
        sb.append(", token=").append(token);
        sb.append(", last_used=").append(last_used);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}