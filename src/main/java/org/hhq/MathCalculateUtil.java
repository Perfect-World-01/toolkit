package com.ouyeel.platform.components.credit.manager.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * @see BigDecimal
 * @see Math
 * @see Constructor
 * @see Method
 * @see java8.lambda
 * {@code calculate} 计算的总起，提供外部访问
 * {@code replaceParamsValues} 变量替换，提供外部访问
 * {@code isValidExpression} 表达式校验，目前只提供基本校验
 * {@code splitModules} 模块拆分，()分隔的为主模块， ,分隔的为子模块；每次只提取一个主模块，不管是主模块还是子模块都是计算到不含运算符号的结果值为止
 * 主模块用于分块，和进行对应于math中方法的运算{@code perfectCalculate}
 * 子模块用于分块，和进行对应于运算符号的值计算{@code arithmeticSplit},{@code arithmeticSort},{@code doCalculate}
 * <p>
 * 日志：{@code logger} 提供info级别的日志记录 {@code level} 用于splitModules中指定当前模块层级
 * <p>
 * 如果需要扩展{@see MathCalculateUtil}：
 * 1、如果你只是想增加运算规则:
 * 对应于双目运算符
 * {@code arithmeticPriority}存储运算规则，以及运算优先级
 * 对应于:BigDecimal
 * {@code calauteMap}存储运算规则，以及运算方法
 * 对应于Math：
 * {@code prefix}存储运算方法，以及运算方法的使用规则
 * {@code suffix}存储运算方法，以及运算方法的使用规则
 * 2、如果你想扩展规则(用于变量运算):
 * 对应于除双目运算符外的其他运算符
 * 建议扩展功能紧跟{@code replaceParamsValues}
 * <p>
 * 该类为数学计算工具类型，支持较低级的表达式计算：如下
 * String expression = "tan(1+2+1.0+1+1+1+1000.10001+(100.0+10+19/20*6+5)+min(100,100.2,99,max(1,2,3,4,5,6,100,10000,1,19999,12,18000))+{pam})";
 * Map<String, Object> map = new HashMap<>();
 * map.put("{pam}",100);
 * MathCalculateUtil.calculate(expression, map);
 */
public class MathCalculateUtil {

    /**
     * 存储算法优先级，用于算法排序
     */
    private static final Map<String, Integer> arithmeticPriority = new HashMap<>();

    static {
        //key,操作类型，value，优先级
        arithmeticPriority.put("*", 111);
        arithmeticPriority.put("/", 111);
        arithmeticPriority.put("%", 111);
        arithmeticPriority.put("+", 222);
        arithmeticPriority.put("-", 222);
    }

    /**
     * 存储计算规则
     */
    private static final Map<String, String> calauteMap = new HashMap<>();

    static {
        //key,操作类型，value:对应于BigDecimal方法
        calauteMap.put("*", "multiply");
        calauteMap.put("/", "divide");
        calauteMap.put("%", "remainder");
        calauteMap.put("+", "add");
        calauteMap.put("-", "subtract");
    }

    /**
     * 日志
     */
    private static Logger logger = Logger.getLogger(MathCalculateUtil.class);

    /**
     * @param expression 表达式
     * @return 计算结果
     * @throws Exception
     */
    public static String calculate(String expression) throws Exception {
        return calculate(expression, null);
    }

    /**
     * 表达式计算
     *
     * @param expression 表达式
     * @param paramMap   变量映射
     * @return 计算结果
     */
    public static String calculate(String expression, Map<String, Object> paramMap) throws Exception {
        level.set(0);
        expression = "(" + expression + ")";
        expression = replaceParamsValues(expression, paramMap);
        if (logger.isInfoEnabled()) {
            logger.log(Level.INFO, "calculate:变量替换后的表达式：" + expression);
        }
        assert StringUtils.isEmpty(expression) || expression.matches("(\\{|\\}|\\[|\\])+") : "表达式错误";
        expression = splitModules(expression);
        return expression;
    }

    /**
     * 消除传入参数
     *
     * @param expression 含有参数的表达式
     * @param paramMap   传入参数映射
     * @return 返回真知表达式
     */
    public static String replaceParamsValues(String expression, Map<String, Object> paramMap) throws Exception {
        if (MapUtils.isNotEmpty(paramMap) && isValidExpression(expression)) {
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                if (expression.contains(entry.getKey())) {
                    if (logger.isInfoEnabled()) {
                        logger.log(Level.INFO, "replaceParamsValues:当前变量：" + entry.getKey() + ";变量值：" + entry.getValue());
                    }
                    expression = expression.substring(0, expression.indexOf(entry.getKey())) + paramsValues(entry.getValue()) + expression.substring(expression.indexOf(entry.getKey()) + entry.getKey().length());
                }
            }
        }
        if (expression != null && expression.contains("{")) {
            throw new Exception("表达式错误：MathCalculateUtil:replaceParamsValues(" + expression + "," + paramMap.toString() + ")");
        }
        return expression;
    }

    private static String paramsValues(Object object) {
        DecimalFormat decimalFormat = new DecimalFormat();
        BigDecimal bigDecimal = null;
        if (object instanceof BigDecimal) {
            bigDecimal = (BigDecimal) object;
        } else if (object instanceof Integer) {
            bigDecimal = new BigDecimal((Integer) object);
        } else if (object instanceof Float) {
            bigDecimal = new BigDecimal((Float) object);
        } else if (object instanceof Double) {
            bigDecimal = new BigDecimal((Double) object);
        } else {
            return decimalFormat.format(object);
        }
        return decimalFormat.format(bigDecimal);
    }

    private static final Map<String, Object> prefix = new HashMap<>();

    static {
        //key对应于math中的方法，value：第一个参数为允许使用的参数个数，第二个为是否将比较的值加入下一次比较
        prefix.put("max", new int[]{2, 1});
        prefix.put("min", new int[]{2, 1});
        prefix.put("abs", new int[]{1});
        prefix.put("pow", new int[]{2});
        prefix.put("floor", new int[]{1});
        prefix.put("tan", new int[]{1});
    }

    private static final Set<String> suffix = new HashSet<>();

    private static AtomicInteger level = new AtomicInteger(1);

    /**
     * 对原始表达式进行分模块
     *
     * @param expression 真值表达式
     * @return 返回最简表达式
     */
    //以()和,进行分模块：主模块() 子模块，
    protected static String splitModules(String expression) throws Exception {
        if (isValidExpression(expression)) {
            //模块开始点
            int start = expression.lastIndexOf("(");
            //临时点
            String tempPoint = expression.substring(expression.lastIndexOf("("));
            //模块结束点
            int end = tempPoint.indexOf(")");
            //前半段
            String prefixStr = expression.substring(0, start);
            //后半段
            String suffixStr = tempPoint.substring(end + 1);
            //不需要括号
            String values = tempPoint.substring(1, end);
            //获取前缀
            String valuesPrefix = "";
            //过滤的结果
            Optional<String> filterResult = null;
            if (MapUtils.isNotEmpty(prefix)) {
                final String tempPrefixStr = prefixStr;
                Predicate<String> filter = str -> StringUtils.isNotEmpty(str) && tempPrefixStr.lastIndexOf(str) + str.length() == tempPrefixStr.length();
                filterResult = prefix.keySet().stream().filter(filter).findFirst();
                if (filterResult.isPresent() && StringUtils.isNotEmpty(filterResult.get()))
                    valuesPrefix = filterResult.get();
                if (StringUtils.isNotEmpty(valuesPrefix))
                    prefixStr = prefixStr.substring(0, prefixStr.lastIndexOf(valuesPrefix));
            }
            //获取后缀
            String valuesSuffix = "";
            if (CollectionUtils.isNotEmpty(suffix)) {
                final String tempSuffixStr = suffixStr;
                Predicate<String> filter = str -> StringUtils.isNotEmpty(str) && tempSuffixStr.indexOf(str) == 0;
                filterResult = suffix.stream().filter(filter).findFirst();
                if (filterResult.isPresent() && StringUtils.isNotEmpty(filterResult.get()))
                    valuesSuffix = filterResult.get();
                if (StringUtils.isNotEmpty(valuesSuffix))
                    suffixStr = suffixStr.substring(suffixStr.lastIndexOf(valuesPrefix));
            }
            StringBuilder builder = new StringBuilder();
            String[] valueArray = values.split(",");
            // , 的模块分发
            for (int i = 0; i < valueArray.length; i++) {
                String value = valueArray[i];
                Predicate<String> filter = key -> StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(value) && value.contains(key);
                //对每个包括在内进行计算
                if (arithmeticPriority.keySet().stream().anyMatch(filter)) {
                    //获取计算符
                    String[] arithmetic = arithmeticSplit(value, "(\\d|\\.)+");
                    //计算符排序
                    arithmeticSort(arithmetic);
                    //初步计算
                    valueArray[i] = doCalculate(value, arithmetic);
                }
                if (i < valueArray.length - 1) {
                    builder.append(valueArray[i]).append(",");
                } else {
                    builder.append(valueArray[i]);
                }
            }
            if (logger.isInfoEnabled()) {
                logger.info("splitModules:第" + level.incrementAndGet() + "层表达式:" + values + "，子表达式的计算结果：" + builder.toString());
            }
            //算出结果
            values = perfectCalculate(valuesPrefix, builder.toString(), valuesSuffix);
            //计算后的表达式
            expression = prefixStr + values + suffixStr;
            if (logger.isInfoEnabled()) {
                logger.info("splitModules:第" + level.get() + "层，最新原表达式:" + expression);
            }
            //不放过每一层()
            if (expression.contains("(") && isValidExpression(expression)) {
                return splitModules(expression);
            }
        }
        return expression;
    }

    /**
     * 算法分割
     *
     * @param expression 或最简表达式
     * @param regexp     分割方式
     * @return
     */
    private static String[] arithmeticSplit(String expression, String regexp) {
        if (StringUtils.isNotEmpty(regexp) && isValidExpression(expression)) {
            String[] expressions = expression.split(regexp);
            //去空值
            int j = -1;
            for (int i = 0, n = 0; i < expressions.length; n = i += 1) {
                if (StringUtils.isEmpty(expressions[i])) {
                    while (++n < expressions.length && expressions[j = n] != null) {
                        expressions[n - 1] = expressions[n];
                        expressions[n--] = null;
                    }
                }
            }
            if (j >= 0) expressions = Arrays.copyOfRange(expressions, 0, j < expression.length() ? j : j - 1);
            return expressions;
        }
        return null;
    }

    /**
     * 算法排序
     *
     * @param arithmetics 需要排序的算法
     */
    private static void arithmeticSort(String[] arithmetics) {
        if (ArrayUtils.isNotEmpty(arithmetics) && MapUtils.isNotEmpty(arithmeticPriority)) {
            Arrays.sort(arithmetics, (str1, str2) -> {
                if (StringUtils.isNotEmpty(str1) && StringUtils.isNotEmpty(str2) && arithmeticPriority.get(str1) != null && arithmeticPriority.get(str2) != null) {
                    if (arithmeticPriority.get(str1) >= arithmeticPriority.get(str2)) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
                return 0;
            });
        }
    }

    /**
     * 计算、化简
     *
     * @param expression       最简表达式
     * @param arithmeticSorted 排序后算法
     */
    private static String doCalculate(String expression, String[] arithmeticSorted) throws Exception {
        if (StringUtils.isNotEmpty(expression) && arithmeticSorted != null) {
            String[] valuesArray = new String[2];
            for (String symbol : arithmeticSorted) {
                valuesArray[0] = null;
                valuesArray[1] = null;
                if (StringUtils.isEmpty(symbol) || !findNumber(expression, symbol, valuesArray)) {
                    continue;
                }
                if (ArrayUtils.isNotEmpty(valuesArray) && StringUtils.isNotEmpty(valuesArray[0]) && StringUtils.isNotEmpty(valuesArray[1])) {
                    String methodName = calauteMap.get(symbol);
                    Constructor<BigDecimal> constructor = BigDecimal.class.getConstructor(String.class);
                    BigDecimal bigDecimal1 = constructor.newInstance(valuesArray[0]);
                    BigDecimal bigDecimal2 = constructor.newInstance(valuesArray[1]);
                    Optional<Method> optional = Arrays.stream(BigDecimal.class.getMethods()).filter(method -> method.getName().contains(methodName) && method.getParameterCount() == 1).findFirst();
                    if (optional.isPresent()) {
                        BigDecimal bigDecimal3 = (BigDecimal) BigDecimal.class.getMethod(optional.get().getName(), optional.get().getParameterTypes()).invoke(bigDecimal1, bigDecimal2);
                        String temp = valuesArray[0] + symbol + valuesArray[1];
                        int index = expression.indexOf(temp);
                        expression = expression.substring(0, index) + bigDecimal3.doubleValue() + expression.substring(index + temp.length(), expression.length());
                    }
                } else {
                    throw new Exception("\n出错了：MathCalculateUtil.doCalculate(" + expression + "," + Arrays.toString(arithmeticSorted) + ")");
                }
            }
        }
        return expression;
    }

    private static boolean findNumber(String expression, String string, String[] valuesArray) {
        boolean check = false;
        if (StringUtils.isNotEmpty(expression) && StringUtils.isNotEmpty(string) && valuesArray != null && valuesArray.length >= 2) {
            String left = expression.substring(0, expression.indexOf(string));
            String right = expression.substring(expression.indexOf(string) + string.length(), expression.length());
            if (StringUtils.isEmpty(left) && StringUtils.isEmpty(right)) {
                return check;
            }
            //从右往左取值
            for (int i = left.length(); i >= 0; i--) {
                if (i == 0 || String.valueOf(left.charAt(i - 1)).matches("[^0-9\\.]+")) {
                    valuesArray[0] = left.substring(i, left.length());
                    check = true;
                    break;
                }
            }
            //左往右取值
            for (int i = 1; i <= right.length(); i++) {
                if (i == right.length() || String.valueOf(right.charAt(i - 1)).matches("[^0-9\\.]+")) {
                    if (i != right.length()) i -= 1;
                    valuesArray[1] = right.substring(0, i);
                    check = true;
                    break;
                }
            }
        }
        return check;
    }

    /**
     * 对结果进行完善
     *
     * @param prefix 前缀
     * @param value  计算的值
     * @return 最简结果
     */
    private static String perfectCalculate(String prefix, String value, String suffix) throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        //每层最多有一层()
        if (StringUtils.isNotEmpty(prefix) && StringUtils.isNotEmpty(value)) {
            Predicate<Method> methodPredicate = method -> method.getName().contains(prefix);
            //float优先
            Comparator<Method> methodComparator1 = (method1, method2) -> {
                if (method1.getReturnType().getSimpleName().equalsIgnoreCase("float")) return -1;
                if (method2.getReturnType().getSimpleName().equalsIgnoreCase("float")) return 1;
                return 0;
            };
            //double优先
            Comparator<Method> methodComparator2 = (method1, method2) -> {
                if (method1.getReturnType().getSimpleName().equalsIgnoreCase("double")) return -1;
                if (method2.getReturnType().getSimpleName().equalsIgnoreCase("double")) return 1;
                return 0;
            };
            Optional<Method> optional = Arrays.stream(Math.class.getMethods()).filter(methodPredicate).sorted(methodComparator1).sorted(methodComparator2).findFirst();
            if (!optional.isPresent()) return value;
            Method method = Math.class.getMethod(optional.get().getName(), optional.get().getParameterTypes());
            if (method == null) return value;
            //暴力创建
            Constructor<Math> constructor = Math.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Math math = constructor.newInstance();
            Object object = MathCalculateUtil.prefix.get(prefix);
            int model = 1;
            boolean useValue = false;
            if (object instanceof int[]) {
                int[] cm = (int[]) object;
                model = cm.length >= 1 ? cm[0] : 1;
                useValue = cm[cm.length >= 2 ? 1 : 0] == 1;
            }
            String simpleName = optional.get().getReturnType().getSimpleName().toUpperCase();
            String result = null;
            Object[] obj = null;
            String[] values = value.split(",");
            for (int i = 1, n = 0, m = 0; i <= values.length && values.length >= model; i++) {
                switch (simpleName) {
                    case "DOUBLE":
                        if (obj == null) obj = new Double[model];
                        if (n == 1) obj[0] = Double.valueOf(result);
                        obj[m] = Double.valueOf(values[i - 1]);
                        break;
                    case "FLOAT":
                        if (obj == null) obj = new Float[model];
                        if (n == 1) obj[0] = Float.valueOf(result);
                        obj[m] = Float.valueOf(values[i - 1]);
                        break;
                    case "LONG":
                        if (obj == null) obj = new Long[model];
                        if (n == 1) obj[0] = Long.valueOf(result);
                        obj[m] = Long.valueOf(values[i - 1]);
                        break;
                    case "INT":
                    case "INEGER":
                        if (obj == null) obj = new Integer[model];
                        if (n == 1) obj[0] = Integer.valueOf(result);
                        obj[m] = Integer.valueOf(values[i - 1]);
                        break;
                    default:
                        break;
                }
                m += 1;
                if (i % model == 0 || m == model) {
                    m = 0;
                    if (useValue) m = n = 1;
                    result = String.valueOf(method.invoke(math, obj));
                }
            }
            if (logger.isInfoEnabled()) {
                logger.info("perfectCalculate:第" + level.get() + "层，操作:" + prefix + ";操作的表达式:" + value + ";操作结果：" + result);
            }
            value = result;
        }
        return value;
    }

    /**
     * 校验表达式
     *
     * @param expression
     * @return
     */
    private static boolean isValidExpression(String expression) {
        boolean check = false;
        if (StringUtils.isNotEmpty(expression)) {
            //验证非运算符号
            if (expression.contains("`") || expression.contains("@") || expression.contains("#") || expression.contains("$") || expression.contains("_") || expression.contains("=") || expression.contains("、") || expression.contains("\\") || expression.contains("?") || expression.contains(";") || expression.contains(":")) {
                check = true;
            }
            //验证()
            if (expression.contains("(") && !check) {
                expression = expression.replaceAll("\\(", "\\(`");
                expression = expression.replaceAll("\\)", "\\)`");
                if (expression.split("\\(").length != expression.split("\\)").length) {
                    check = true;
                }
            }
            //验证{}
            if (expression.contains("{") && !check) {
                expression = expression.replaceAll("\\{", "\\{`");
                expression = expression.replaceAll("}", "\\}`");
                if (expression.split("\\{").length != expression.split("}").length) {
                    check = true;
                }
            }
            //验证[]
            if (expression.contains("[") && !check) {
                expression = expression.replaceAll("\\[", "\\[`");
                expression = expression.replaceAll("]", "\\]`");
                if (expression.split("\\[").length != expression.split("]").length) {
                    check = true;
                }
            }
            return !check;
        }
        return check;
    }

    public static void main(String[] args) throws Exception {
        String expression = "tan(1+2+1.0+1+1+1+1000.10001+(100.0+10+19/20*6+5)+min(100,100.2,99,max(1,2,3,4,5,6,100,10000,1,19999,12,18000))+{pam})";
        Map<String, Object> map = new HashMap<>();
        map.put("{pam}", 100);
        System.out.println("value:" + MathCalculateUtil.calculate(expression, map));
    }
}
