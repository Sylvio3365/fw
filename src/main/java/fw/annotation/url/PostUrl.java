package fw.annotation.url;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import fw.helper.HttpMethods;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostUrl {
    String value() default "";
    String method() default HttpMethods.POST;
}