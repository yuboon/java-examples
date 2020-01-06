import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;

/**
* 用户信息-控制层
*
* @author yuboon
* @version v1.0
* @date 2020-01-05
*/
@RestController
public class SysUserController {

    @Autowired
    private SysUserService SysUserService;

}