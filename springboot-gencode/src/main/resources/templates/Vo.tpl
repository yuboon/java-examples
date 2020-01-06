import cn.wp.common.core.dao.annotation.Column;
import cn.wp.common.core.dao.annotation.Table;
import lombok.Data;

import java.io.Serializable;

/**
* [(${comment})]-实体
*
* @author yuboon
* @version v1.0
* @date [(${createDate})]
*/
@Data
public class [(${className})]Vo implements Serializable {
    [# th:each="column : ${columnList}"]
    /** [(${column.comment})]  */
    @Column("[(${column.code})]")
    private [(${column.type})] [(${column.attrName})];

    [/]
}
