import cn.wp.common.core.dao.annotation.Column;
import cn.wp.common.core.dao.annotation.Table;
import lombok.Data;

import java.io.Serializable;

/**
* 用户信息-实体
*
* @author yuboon
* @version v1.0
* @date 2020-01-05
*/
@Data
public class SysUserVo implements Serializable {
    
    /** 主键  */
    @Column("id")
    private int id;

    /** 姓名  */
    @Column("name")
    private String name;

    
}
