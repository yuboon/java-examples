import lombok.Data;

import java.io.Serializable;

/**
* 用户信息-实体
*
* @author yuboon
* @version v1.0
* @date 2020-01-08
*/
@Data
public class SysUser implements Serializable {
    
    /** 主键  */
    @Column("id")
    private int id;

    /** 姓名  */
    @Column("name")
    private String name;

    
}
