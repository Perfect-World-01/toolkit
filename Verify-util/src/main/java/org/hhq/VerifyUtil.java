package org.hhq;

import org.hhq.annotation.verify.NotEmpty;
import org.hhq.annotation.verify.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    private static final Map<String, String[]> verifyMap;
    /**
     * 本地校验方法
     * key:方法
     * value:参数类型数组
     */
    private static final Map<String, Class<?>[]> methodMap;
    /**
     * 对map进行排序
     * key:方法
     * value:优先级
     * 优先级越小排在越前
     */
    private static final Map<String,Integer> sortMap;
    /**
     * 注解所在包名
     * 当前校验类,目前只适用于{@code #PACKAGE_NAME}所在包
     */
    private static final String PACKAGE_NAME = "org.hhq";
    /**
     * 定义当前类名
     */
    private static final Class<?> VERIFYUTIL_CLASS = VerifyUtil.class;

    static {
        VERIFY_UTIL = new VerifyUtil();
        verifyMap = new HashMap<>();
        verifyMap.put("NotEmpty", new String[]{"isNotEmpty"});
        verifyMap.put("NotNull", new String[]{"isNotNull"});
        verifyMap.put("Range", new String[]{"range", "max", "min"});
        verifyMap.put("Size", new String[]{"size", "max", "min"});
    }

    static {
        methodMap = new HashMap<>();
        methodMap.put("isNotEmpty", new Class[]{Object.class});
        methodMap.put("isNotNull", new Class[]{Object.class});
        methodMap.put("range", new Class[]{Object.class, Long.class, Long.class});
        methodMap.put("size", new Class[]{Object.class, Long.class, Long.class});
    }

    static {
        sortMap = new HashMap<>();
        sortMap.put("NotNull",1);
        sortMap.put("NotEmpty",2);
        //下面两个不需要重新排序
        sortMap.put("Range",3);
        sortMap.put("Size",3);
    }

    /**
     * 校验方法
     *
     * @param verifyObject     校验对象
     * @param describeParam    如果没有校验描述的时候选用该描述
     * @param annotationVerify 仅仅进行注解校验
     * @param <T>              泛型扩展
     * @throws Exception       校验结果通过异常方式抛出
     */
    public static <T> void verify(T verifyObject, String describeParam, boolean annotationVerify) throws Exception {
        assertNull(verifyObject);
        Class cls = verifyObject.getClass();
        Field[] fields = cls.getDeclaredFields();
        Method method = null;
        Object object =  null;
        String[] paramStr = null;
        Object[] paramObj = null;
        for (Field field : fields) {
            //方便field获取值
            field.setAccessible(true);
            //添加额外注解
            Collection<Annotation> annotations = getAdditionalAnnotations(field.getDeclaredAnnotations());
            //本注解
            annotations.addAll(Arrays.asList(field.getDeclaredAnnotations()));
            //过滤以及排序
            annotations = filterAndSortedAnnotation(annotations);
            for (Annotation annotation : annotations) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (verifyMap.containsKey(annotationType.getSimpleName())) {
                    paramStr = verifyMap.get(annotationType.getSimpleName());
                    //调用本类方法需要传入的参数
                    paramObj = new Object[paramStr.length];
                    //第一个参数值
                    paramObj[0] = field.get(verifyObject);
                    for (int j = 1; j < paramStr.length; j++) {
                        method = annotationType.getDeclaredMethod(paramStr[j]);
                        paramObj[j] = method.invoke(annotation);
                    }
                    //允许否定注解
                    object = annotationType.getDeclaredMethod("value").invoke(annotation);
                    if(!isNotEmpty(paramObj[0])){
                        if(annotation instanceof NotEmpty){
                            throw new Exception(((NotEmpty)annotation).describe());
                        }else if(annotation instanceof NotNull){
                            throw new Exception(((NotNull)annotation).describe());
                        }else{
                            throw new Exception(String.format("%s:值为空",field.getName()));
                        }
                    }else if ((boolean) object != (boolean) getMethod(annotationType.getSimpleName(), methodMap.get(paramStr[0])).invoke(VERIFY_UTIL, paramObj)) {
                        method = annotationType.getDeclaredMethod("describe");
                        Object describe = method.invoke(annotation);
                        if (describe == null || (describe instanceof String && ((String) describe).trim().isEmpty())) {
                            throw new VerifyError(String.format("使用注解:%s 却未提供describe", annotationType.getName()));
                        }
                        throw new Exception(describe.toString());
                    }
                }
            }
            //如果没有进入注解循环
            if (!annotationVerify) {
                if (field.getType().getSimpleName().equalsIgnoreCase("String")) {
                    method = getMethod("NotEmpty", Object.class);
                } else {
                    method = getMethod("NotNull", Object.class);
                }
                if (!(boolean) method.invoke(VERIFY_UTIL, field.get(verifyObject))) {
                    if (!isNotEmpty(describeParam)) {
                        describeParam="值为空";
                    }
                    throw new Exception(describeParam);
                }
            }
            //判断是否为非普通类型
            if (!checkIsGeneralType(field.getType(),field.get(verifyObject))) {
                //统一通过集合操作信息
                if((object=returnCollections(field.get(verifyObject)))!=null){
                    operateCollection(object, describeParam, annotationVerify);
                }
            }
            field.setAccessible(false);
        }
    }

    /**
     * 获取额外的注解
     *
     * @param annotations 当前含有的注解
     * @return 额外注解
     */
    private static Collection<Annotation> getAdditionalAnnotations(Annotation[] annotations) {
        Set<Annotation> annotationSets = new HashSet<>();
        Arrays.stream(annotations).forEach(annotation -> {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            Predicate<Method> filter = method -> method != null && method.getReturnType().isAnnotation();
            //仅扩展一层
            Arrays.stream(annotationType.getDeclaredMethods()).filter(filter).forEach(method -> {
                try {
                    annotationSets.add((Annotation) method.invoke(annotation));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
        });
        return annotationSets;
    }

    /**
     * 对注解进行过滤和排序
     * @param paramMap  需要过滤及排序的注解
     * @return  过滤并排序后的注解
     */
    private static Collection<Annotation> filterAndSortedAnnotation(Collection<Annotation> paramMap){
        return paramMap.stream().filter((annotation)->
                annotation.annotationType().getPackage().getName().startsWith(PACKAGE_NAME)
        ).sorted((key1,key2)->{
            if(sortMap.get(key1.annotationType().getSimpleName())>sortMap.get(key2.annotationType().getSimpleName())){
                return 1;
            }else if(sortMap.get(key1.annotationType().getSimpleName())<sortMap.get(key2.annotationType().getSimpleName())){
                return -1;
            }else{
                return 0;
            }
        }).collect(Collectors.toList());
    }

    /**
     * 判断是否为普通类型
     *
     * @param cls 类名
     * @param object 对象
     * @return boolean, true:普通类型，false:可能是集合类型
     */
    private static boolean checkIsGeneralType(Class<?> cls,Object object) {
        String simpleName = cls.getSimpleName();
        //当Object不为空时
        if(object instanceof Number || object instanceof String || object instanceof Date){
            return true;
        }else if (simpleName.equalsIgnoreCase("byte") || simpleName.equalsIgnoreCase("boolean") || simpleName.equalsIgnoreCase("int") || simpleName.equalsIgnoreCase("long") || simpleName.equalsIgnoreCase("double")) {
            return true;
        } else if (simpleName.equalsIgnoreCase("Integer") || simpleName.equalsIgnoreCase("BigInteger") || simpleName.equalsIgnoreCase("BigDecimal")) {
            return true;
        } else if (simpleName.equalsIgnoreCase("String") || simpleName.equalsIgnoreCase("date") || simpleName.equalsIgnoreCase("object")) {
            return true;
        }
        return false;
    }

    /**
     * 校验集合类型
     *
     * @param object 需要判断的对象
     * @return Object：集合类型，or：null
     */
    private static Object returnCollections(Object object) {
        if(isNotEmpty(object)){
            if(object instanceof Map){
                return ((Map) object).values();
            }else if(object instanceof Collection){
                return object;
            }else{
                return Arrays.asList(object);
            }
        }
        return null;
    }

    /**
     * 操作集合
     *
     * @param object           属性值(集合)
     * @param describeParam    如果annotationVerify:true时,没有标记注解的属性返回描述结果
     * @param annotationVerify 是否只校验注解
     * @throws Exception 出现异常
     */
    @SuppressWarnings("unchecked")
    private static void operateCollection(Object object, String describeParam, boolean annotationVerify) throws Exception {
        if (object instanceof Collection) {
            Collection<?> collection = (Collection<?>) object;
            for (Object objectValue : collection) {
                verify(objectValue, describeParam, annotationVerify);
            }
        }
    }



    private static Method getMethod(String methodName, Class<?>... classes) throws Exception {
        assertEmpty(verifyMap.get(methodName) == null || verifyMap.get(methodName).length <= 0, "传入方法名称为空");
        return VERIFYUTIL_CLASS.getMethod(verifyMap.get(methodName)[0], classes);
    }

    /**
     * empty 类型
     * 对于String 或者集合类型进行非空判断
     *
     * @param object 判断对象
     * @return 判断结果
     */
    public static boolean isNotEmpty(Object object) {
        if (object instanceof String && ((String) object).isEmpty()) {
            return false;
        } else if (object instanceof Collection && ((Collection) object).isEmpty()) {
            return false;
        } else if (object instanceof Map && ((Map) object).isEmpty()) {
            return false;
        } else {
            return isNotNull(object);
        }
    }


    /**
     * nulll 类型
     *
     * @param object 判断对象
     * @return 判断结果
     */
    public static boolean isNotNull(Object object) {
        if (object == null) {
            return false;
        }
        return true;
    }


    /**
     * 适用于数值
     * rang 类型
     *
     * @param object 判断对象
     * @param max    最大值
     * @param min    最小值
     * @return 判断结果
     */
    public static boolean range(Object object, Long max, Long min) {
        assertNull(object);
        Double tempValue = 0.0;
        min = (min==null || min<0) ? 0 : min;
        max = (max==null || max<min) ? Long.MAX_VALUE : max;
        if (!(object instanceof Number) && !(object instanceof String)) {
            throw new VerifyError(String.format("参数类型异常->请求%s、%s类型，传入%s类型", Number.class.toString(), String.class.toString(), object.getClass().toString()));
        }
        if (object instanceof Byte && ((Byte) object > max || (Byte) object < min)) {
            return false;
        } else if (object instanceof Integer && ((Integer) object > max || (Integer) object < min)) {
            return false;
        } else if (object instanceof Float && ((Float) object > max || (Float) object < min)) {
            return false;
        } else if (object instanceof Double && ((Double) object > max || (Double) object < min)) {
            return false;
        } else {
            if (object instanceof String) {
                if ((tempValue = Double.valueOf(String.valueOf(object))) > max || tempValue < min) {
                    return false;
                }
                return true;
            } else {
                return (((Number) object).doubleValue() <= max) && (((Number) object).doubleValue() >= min);
            }
        }
    }

    /**
     * size 类型
     *
     * @param object 判断对象
     * @param max    最大长度
     * @param min    最小长度
     * @return 判断结果
     */
    public static boolean size(Object object, Long max, Long min) {
        assertNull(object);
        min = (min==null || min<0) ? 0 : min;
        max = (max==null || max<min) ? Long.MAX_VALUE : max;
        if (!(object instanceof Number) && !(object instanceof String) && !(object instanceof Collection) && !(object instanceof Map)) {
            throw new VerifyError(String.format("参数类型异常->请求Number、String、Collection或Map类型，传入%s类型", object.getClass().toString()));
        }
        if (object instanceof String && (((String) object).length() > max || ((String) object).length() < min)) {
            return false;
        } else if (object instanceof Collection && (((Collection) object).size() > max || ((Collection) object).size() < min)) {
            return false;
        } else if (object instanceof Map && (((Map) object).size() > max || ((Map) object).size() < min)) {
            return false;
        } else if (object instanceof Number && (String.valueOf(object).length()>max || String.valueOf(object).length()<min)) {
            return false;
        }
        return true;
    }

    /**
     * null断言
     *
     * @param object 判断对象
     * @return 判断结果或NullPointerException
     */
    public static boolean assertNull(Object object) {
        if (!isNotNull(object)) {
            throw new NullPointerException();
        }
        return false;
    }

    /**
     * empty断言
     *
     * @param object 判断对象
     * @return 判断结果或NullPointerException
     */
    public static boolean assertEmpty(Object object) {
        if (!isNotEmpty(object)) {
            throw new NullPointerException();
        }
        return false;
    }

    /**
     * empty断言
     *
     * @param bool 判断对象
     * @return 判断结果或NullPointerException
     */
    public static boolean assertEmpty(boolean bool, String... messages) throws Exception {
        if (bool) {
            StringBuilder builder = null;
            if (messages != null && messages.length > 0) {
                builder = new StringBuilder();
                for (String message : messages) {
                    builder.append(message);
                    builder.append("\n");
                }
            } else {
                builder = new StringBuilder("校验值为空");
            }
            throw new Exception(builder.toString());
        }
        return false;
    }
}
