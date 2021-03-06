package org.hhq.annotation.verify;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.FIELD;

/** 自定义非empty注解
 * Inherited            支持继承
 * Documented           允许生成文档
 * Target[FIELD]        适用范围为字段
 * Retention[RUNTIME]   可用时期为运行时
 * @author huhaiqing
 */
@Inherited
@Documented
@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotEmpty {
    /**
     * 对注解进行肯定或否定
     * @return 是否直接支持注解
     */
    boolean value() default true;

    /**
     * 注解判断后的描述
     * @return 描述信息
     */
    String describe() default "";
}
