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
public class [(${className})] implements Serializable {
    [# th:each="column : ${columnList}"]
    /** [(${column.comment})]  */
    @Column("[(${column.code})]")
    private [(${column.type})] [(${column.attrName})];

    [/]
}
