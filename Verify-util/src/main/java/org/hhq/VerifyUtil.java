package org.hhq;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author huhaiqing
 */
public class VerifyUtil {
    /**
     * 创建一个本地对象
     * 用于调用本地方法
     */
    private static final VerifyUtil VERIFY_UTIL;
    /**
     * 校验注解扩展
     * key:注解名称
     * value:注解对应的本地方法，第一位为方法名，后面的为除该方法名对应方法的第一个参数外的参数名称(其实是与注解的部分方法名称对应的)
     */
    private static final Map<String,String[]> verifyMap;
    /**
     * 本地校验方法
     * key:方法
     * value:参数类型数组
     */
    private static final Map<String,Class<?>[]> methodMap;

    static {
        VERIFY_UTIL = new VerifyUtil();
        verifyMap = new HashMap<>();
        verifyMap.put("NotEmpty",new String[]{"isNotEmpty"});
        verifyMap.put("NotNull",new String[]{"isNotNull"});
        verifyMap.put("Range",new String[]{"range","max","min"});
        verifyMap.put("Size",new String[]{"size","max","min"});
    }

    static {
        methodMap = new HashMap<>();
        methodMap.put("isNotEmpty",new Class[]{Object.class});
        methodMap.put("isNotNull",new Class[]{Object.class});
        methodMap.put("range",new Class[]{Object.class,Integer.class,Integer.class});
        methodMap.put("size",new Class[]{Object.class,Integer.class,Integer.class});
    }

    /**
     * 校验方法
     * @param t                 校验对象
     * @param describeParam     如果没有校验描述的时候选用该描述
     * @param <T>               泛型扩展
     * @return                  检验结果Map 或者 null
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchFieldException
     * @throws InstantiationException
     */
    public static <T> Map<String,Object> verify(T t,String describeParam) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, InstantiationException {
        assertNull(t);
        Class cls = t.getClass();
        Field[] fields = cls.getDeclaredFields();
        Method method = null;
        Object object = null;
        String[] paramStr = null;
        Object[] paramObj = null;
        Map<String,Object> result = new HashMap<>();
        for (Field field:fields){
            //方便field获取值
            field.setAccessible(true);
            //即使没有注解也不会为null
            Annotation[] annotations = field.getDeclaredAnnotations();
            for(Annotation annotation : annotations){
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if(verifyMap.containsKey(annotationType.getSimpleName())){
                    paramStr = verifyMap.get(annotationType.getSimpleName());
                    //调用本类方法需要传入的参数
                    paramObj = new Object[paramStr.length];
                    //第一个参数值
                    paramObj[0]=field.get(t);
                    for(int j=1;j<paramStr.length;j++){
                        method = annotationType.getDeclaredMethod(paramStr[j]);
                        paramObj[j] = method.invoke(annotation);
                    }
                    //允许否定注解
                    object = annotationType.getDeclaredMethod("value").invoke(annotation);
                    if((boolean)object != (boolean)VerifyUtil.class.getMethod(paramStr[0],methodMap.get(paramStr[0])).invoke(VERIFY_UTIL,paramObj)){
                        method = annotationType.getDeclaredMethod("describe");
                        Object describe = method.invoke(annotation);
                        if(describe instanceof String && !((String) describe).trim().isEmpty()){
                            result.put(field.getName(),describe);
                        }else if(describe == null || (describe instanceof String && ((String)describe).trim().isEmpty())){
                            throw new VerifyError(String.format("使用注解:%s 却未提供describe",annotationType.getName()));
                        }else{
                            result.put(field.getName(),describe.toString());
                        }
                        return result;
                    }
                }
            }
            //如果没有进入注解循环
            if(object==null){
                if(field.getType().getSimpleName().equalsIgnoreCase("String")){
                    method = VerifyUtil.class.getMethod(verifyMap.get("NotEmpty")[0],Object.class);
                }else{
                    method = VerifyUtil.class.getMethod(verifyMap.get("NotNull")[0],Object.class);
                }
                if(!(boolean)method.invoke(VERIFY_UTIL,field.get(t))){
                    if(isNotEmpty(describeParam)){
                        //获取传入的校验描述
                        result.put(field.getName(),describeParam);
                    }else {
                        result.put(field.getName(),"值为空");
                    }
                    return result;
                }
            }
            object=null;
        }
        return null;
    }
    /**
     * empty 类型
     * 对于String 或者集合类型进行非空判断
     * @param object 判断对象
     * @return 判断结果
     */
    public static boolean isNotEmpty(Object object){
        if(object instanceof String && ((String) object).isEmpty()){
            return false;
        }else if(object instanceof Collection && ((Collection) object).isEmpty()){
            return false;
        }else if(object instanceof Map && ((Map) object).isEmpty()){
            return false;
        }else{
            return isNotNull(object);
        }
    }

    /**
     * nulll 类型
     * @param object 判断对象
     * @return 判断结果
     */
    public static boolean isNotNull(Object object){
        if(object == null){
            return false;
        }
        return true;
    }

    /**
     * rang 类型
     * @param object    判断对象
     * @param max       最大值
     * @param min       最小值
     * @return 判断结果
     */
    public static boolean range(Object object, Integer max, Integer min){
        assertNull(object);
        assertNull(max);
        assertNull(min);
        if(!(object instanceof Number)){
            throw new VerifyError(String.format("参数类型异常->请求%s类型，传入%s类型",Number.class.toString(),object.getClass().toString()));
        }
        if(object instanceof Byte && ((Byte)object>max || (Byte)object<min)){
            return false;
        }else if(object instanceof Integer && ((Integer)object>max || (Integer)object<min)){
            return false;
        }else if(object instanceof Float && ((Float)object>max || (Float)object<min)){
            return false;
        }else if(object instanceof Double && ((Double)object>max || (Double)object<min)){
            return false;
        }
        return (((Number)object).doubleValue()<=max) && (((Number)object).doubleValue()>=min);
    }

    /**
     * size 类型
     * @param object    判断对象
     * @param max       最大长度
     * @param min       最小长度
     * @return 判断结果
     */
    public static boolean size(Object object, Integer max, Integer min){
        assertNull(object);
        assertNull(max);
        assertNull(min);
        if(!(object instanceof String) && !(object instanceof Collection) && !(object instanceof Map)){
            throw new VerifyError(String.format("参数类型异常->请求String、Collection或Map类型，传入%s类型",object.getClass().toString()));
        }
        if(object instanceof String && (((String) object).length()>max || ((String) object).length()<min)){
            return false;
        }else if(object instanceof Collection && (((Collection) object).size()>max || ((Collection) object).size()<min)){
            return false;
        }else if(object instanceof Map && (((Map) object).size()>max || ((Map) object).size()<min)){
            return false;
        }
        return true;
    }

    /**
     * null断言
     * @param object 判断对象
     * @return 判断结果或NullPointerException
     */
    public static boolean assertNull(Object object){
        if(!isNotNull(object)){
            throw new NullPointerException();
        }
        return false;
    }

    /**
     * empty断言
     * @param object 判断对象
     * @return 判断结果或NullPointerException
     */
    public static boolean assertEmpty(Object object){
        if(!isNotEmpty(object)){
            throw new NullPointerException();
        }
        return false;
    }
}
