package org.hhq;

import java.lang.reflect.InvocationTargetException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws InvocationTargetException, NoSuchMethodException, NoSuchFieldException, InstantiationException, IllegalAccessException {
        Verify verify = new Verify();
        System.out.println(VerifyUtil.verify(verify,"字段值为空"));

        System.out.println( "Hello World!" );
    }
}
