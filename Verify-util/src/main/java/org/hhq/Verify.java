package org.hhq;

import org.hhq.annotation.StringVerify;
import org.hhq.annotation.verify.NotEmpty;
import org.hhq.annotation.verify.NotNull;
import org.hhq.annotation.verify.Range;
import org.hhq.annotation.verify.Size;

public class Verify {
    @StringVerify(size = @Size(describe = "Sizevf qefi qen rm", min = 1, max = 2), notEmpty = @NotEmpty(describe = "NotEmptyqefbqernoqemr"),
            notNull = @NotNull(describe = "NotNullerbqerbnqoermb"))
    private String describe="ininomo";

    @NotEmpty(describe = "名称不能为空")
    private String name;
    @Range(max = 20, min = 1, describe = "kkk")
    private int age;

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
