package org.hhq.annotation;

import org.hhq.annotation.verify.NotEmpty;
import org.hhq.annotation.verify.NotNull;
import org.hhq.annotation.verify.Size;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.FIELD;

/**
 * 仅在该处占位作用
 * 方便以后扩展包或类
 */
@Inherited
@Documented
@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StringVerify {
    /**
     * 规定长度
     * @return
     */
    Size size() default @Size;

    /**
     * 规定是否为 empty
     * @return
     */
    NotEmpty notEmpty() default @NotEmpty;

    /**
     * 规定是否为 null
     * @return
     */
    NotNull notNull() default @NotNull;
}
