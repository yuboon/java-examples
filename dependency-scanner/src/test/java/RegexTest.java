import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexTest {
    public static void main(String[] args) {
        try {
            // 测试当前的正则表达式
            String regex = "^(\\[|\\()?([^,\\[\\]\\(\\)]*)?(?:,([^,\\[\\]\\(\\)]*))?(\\]|\\))?$";
            Pattern pattern = Pattern.compile(regex);
            System.out.println("正则表达式编译成功: " + regex);
            
            // 修复后的正则表达式
            String fixedRegex = "^(\\[|\\()?([^,\\[\\]\\(\\)]*)?(?:,([^,\\[\\]\\(\\)]*))?(\\]|\\))?$";
            Pattern fixedPattern = Pattern.compile(fixedRegex);
            System.out.println("修复后的正则表达式编译成功: " + fixedRegex);
            
        } catch (PatternSyntaxException e) {
            System.err.println("正则表达式语法错误: " + e.getMessage());
            System.err.println("错误位置: " + e.getIndex());
            System.err.println("模式: " + e.getPattern());
        }
    }
}