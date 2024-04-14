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
 * @TableName activeNote
 */
@TableName(value ="activeNote")
@Data
public class ActiveNote implements Serializable {
    /**
     * 
     */
    @TableId(value = "noteId")
    private String noteId;

    /**
     * 
     */
    @TableField(value = "lastModifiedTime")
    private Date lastModifiedTime;

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
        ActiveNote other = (ActiveNote) that;
        return (this.getNoteId() == null ? other.getNoteId() == null : this.getNoteId().equals(other.getNoteId()))
            && (this.getLastModifiedTime() == null ? other.getLastModifiedTime() == null : this.getLastModifiedTime().equals(other.getLastModifiedTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getNoteId() == null) ? 0 : getNoteId().hashCode());
        result = prime * result + ((getLastModifiedTime() == null) ? 0 : getLastModifiedTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", noteId=").append(noteId);
        sb.append(", lastModifiedTime=").append(lastModifiedTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}