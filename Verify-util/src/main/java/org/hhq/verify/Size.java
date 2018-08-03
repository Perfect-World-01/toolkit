package org.hhq.verify;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.FIELD;

/** 自定义size注解
 * Inherited            支持继承
 * Documented           允许生成文档
 * Target[FIELD]        适用范围为 字段
 * Retention[RUNTIME]   可用时期为运行时
 * @author huhaiqing
 */
@Inherited
@Documented
@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Size {
    /**
     * 对注解进行肯定或否定
     * @return 是否直接支持注解
     */
    boolean value() default true;

    /**
     * @return 最大长度
     */
    int max() default 0;

    /**
     * @return 最小长度
     */
    int min() default 0;

    /**
     * 注解判断后的描述
     * @return 描述信息
     */
    String describe() default "";
}
