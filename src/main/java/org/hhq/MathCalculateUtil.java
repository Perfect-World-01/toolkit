package org.hhq;

import org.apache.commons.lang3.StringUtils;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@SuppressWarnings("all")
public class MathCalculateUtil {

    /**
     * 存储算法优先级，用于算法排序
     */
    private static final Map<String,Integer> arithmeticPriority = new HashMap<>();
    /**
     * 存储计算规则
     */
    private static final Map<String,String> calauteMap = new HashMap<>();

    private static final AtomicInteger level = new AtomicInteger(1000);
    /**
     * 表达式计算
     * @param expression
     * @param paramMap
     * @return
     */
    public static String calculate(String expression,Map<String,Object> paramMap) throws Exception {
        level.set(1000);
        expression = "("+expression+")";
        expression = replaceParamsValues(expression,paramMap);
        expression = splitModules(expression);
        return expression;
    }

    /**
     * 消除传入参数
     * @param expression    含有参数的表达式
     * @param paramMap      传入参数映射
     * @return              返回真知表达式
     */
    public static String replaceParamsValues(String expression,Map<String,Object> paramMap) throws Exception{
        if(paramMap!=null && paramMap.size()>0&&isValidExpression(expression)){
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                if(expression.contains(entry.getKey())){
                    expression.replaceAll(entry.getKey(),paramsValues(entry.getValue()));
                }
            }
            if((expression.contains("{")||expression.contains("["))&&isValidExpression(expression)){
                return replaceParamsValues(expression,paramMap);
            }
        }
        return expression;
    }

    private static String paramsValues(Object object){
        DecimalFormat decimalFormat = new DecimalFormat();
        BigDecimal bigDecimal = null;
        if(object instanceof BigDecimal){
            bigDecimal = (BigDecimal)object;
        }else if(object instanceof Integer){
            bigDecimal = new BigDecimal((Integer)object);
        }else if(object instanceof Float){
            bigDecimal = new BigDecimal((Float)object);
        }else if(object instanceof Double){
            bigDecimal = new BigDecimal((Double)object);
        }else {
            return decimalFormat.format(object);
        }
        return decimalFormat.format(bigDecimal);
    }

    private static final Set<String> prefix = new HashSet<>();

    private static final Set<String> suffix = new HashSet<>();

    /**
     * 对原始表达式进行分模块
     * @param expression    真值表达式
     * @return              返回最简表达式
     */
    protected static String splitModules(String expression) throws Exception {
        if(isValidExpression(expression)){
            String params = "{param"+level.getAndIncrement()+"}";
            int start = expression.lastIndexOf("(");
            String prefixStr = expression.substring(0,start+1);
            int end = expression.indexOf(")");
            String suffixStr = expression.substring(end);
            //不需要括号
            String values = expression.substring(start+1,end);
            String valuesPrefix = "";
            if(prefix!=null&&prefix.size()>0){
                Predicate<String> filter = str-> StringUtils.isNotEmpty(str)&&prefixStr.lastIndexOf(str)+str.length()==prefixStr.length();
                valuesPrefix = prefix.stream().filter(filter).findFirst().get();
            }
            String valuesSuffix = "";
            if(suffix!=null&&suffix.size()>0){
                Predicate<String> filter = str-> StringUtils.isNotEmpty(str)&&suffixStr.indexOf(str)==0;
                valuesSuffix = suffix.stream().filter(filter).findFirst().get();
            }
            //拆分
            String[] arithmetics = arithmeticSplit(values,"(\\d|\\.)*");
            //排序
            arithmeticSort(arithmetics);
            //计算
            values = doCalculate(values,arithmetics);
            //前后缀
            values = valuesPrefix+values+valuesSuffix;
            //算出结果
            values = perfectCalculate(valuesPrefix,values);
            //返回结果
            if(StringUtils.isEmpty(valuesPrefix)){
                expression=prefixStr.substring(0,prefixStr.length()-1);
            }else{
                expression=prefixStr.substring(0,prefixStr.length()-valuesPrefix.length());
            }
            int suffixIndex = suffixStr.indexOf(valuesSuffix)+valuesSuffix.length()+2;
            expression+=values;
            if(StringUtils.isEmpty(valuesSuffix)) {
                expression += suffixStr.substring(1,suffixStr.length());
            }else{
                expression += suffixStr.substring(suffixStr.length()>=suffixIndex?suffixIndex:suffixStr.length());
            }
            if(expression.contains("(")&&isValidExpression(expression)){
                return splitModules(expression);
            }
        }
        return expression;
    }
    /**
     * 算法分割
     * @param expression    或最简表达式
     * @param regexp        分割方式
     * @return
     */
    private static String[] arithmeticSplit(String expression,String regexp){
        if(isValidExpression(expression)){
            //每层最多只有一对{}
            String[] arithmetics = expression.split(regexp);
            return arithmetics;
        }
        return null;
    }

    /**
     * 算法排序
     * @param arithmetics   需要排序的算法
     */
    private static void arithmeticSort(String[] arithmetics){
        if(arithmetics!=null&&arithmetics.length>0&&!Objects.isNull(arithmeticPriority)&&arithmeticPriority.size()>0){
            Arrays.sort(arithmetics,(str1,str2)->{
                if(arithmeticPriority.get(str1)>arithmeticPriority.get(str2)){
                    return 1;
                }else{
                    return -1;
                }
            });
        }
    }

    /**
     * 计算、化简
     * @param expression        最简表达式
     * @param arithmeticSorted  排序后算法
     */
    private static String doCalculate(String expression,String[] arithmeticSorted) throws Exception {
        if(StringUtils.isNotEmpty(expression)&&arithmeticSorted!=null){
            String[] valuesArray = new String[2];
            for(String symbol:arithmeticSorted){
                if(StringUtils.isEmpty(symbol)){
                    continue;
                }
                valuesArray[0]=null;
                valuesArray[1]=null;
                if(!findNumber(expression,symbol,valuesArray)){
                    return "";
                }
                if(valuesArray!=null&&valuesArray.length>=2&&StringUtils.isNotEmpty(valuesArray[0])&&StringUtils.isNotEmpty(valuesArray[0])){
                    BigDecimal bigDecimal = new BigDecimal(valuesArray[0]);
                    Double stringValue=0.0;
                    switch (symbol){
                        case "+":
                            bigDecimal=bigDecimal.add(new BigDecimal(valuesArray[1]));
                            stringValue=bigDecimal.doubleValue();
                            break;
                        case "-":
                            bigDecimal=bigDecimal.subtract(new BigDecimal(valuesArray[1]));
                            stringValue=bigDecimal.doubleValue();
                            break;
                        case "*":
                            bigDecimal=bigDecimal.multiply(new BigDecimal(valuesArray[1]));
                            stringValue=bigDecimal.doubleValue();
                            break;
                        case "/":
                            bigDecimal=bigDecimal.divide(new BigDecimal(valuesArray[1]));
                            stringValue=bigDecimal.doubleValue();
                            break;
                        case "%":
                            bigDecimal=bigDecimal.remainder(new BigDecimal(valuesArray[1]));
                            stringValue=bigDecimal.doubleValue();
                            break;
                        case "^":
                            stringValue = Math.pow(Double.valueOf(valuesArray[0]),Double.valueOf(valuesArray[1]));
                            break;
                        default:
                            break;
                    }
                    String temp = valuesArray[0]+symbol+valuesArray[1];
                    int index = expression.indexOf(temp);
                    expression = expression.substring(0,index)+stringValue+expression.substring(index+temp.length(),expression.length());
                }else if(valuesArray!=null&&valuesArray.length>=2&&(StringUtils.isNotEmpty(valuesArray[0])||StringUtils.isNotEmpty(valuesArray[0]))){
                    throw new Exception("\n出错了：MathCalculateUtil.doCalculate("+expression+","+Arrays.toString(arithmeticSorted)+")");
                }
            }
        }
        return expression;
    }

    private static boolean findNumber(String expression,String string,String[] valuesArray){
        boolean check = false;
        if(StringUtils.isNotEmpty(expression)&&StringUtils.isNotEmpty(string)&&valuesArray!=null&&valuesArray.length>=2){
            String left = expression.substring(0,expression.indexOf(string));
            String right = expression.substring(expression.indexOf(string)+string.length(),expression.length());
            if(StringUtils.isEmpty(left)&&StringUtils.isEmpty(right)){
                return check;
            }
            //从右往左取值
            for(int i=left.length();i>=0;i--){
                if(i==0||String.valueOf(left.charAt(i-1)).matches("[^0-9\\.]+")){
                    valuesArray[0]=left.substring(i,left.length());
                    check=true;
                    break;
                }
            }
            //左往右取值
            for(int i=1;i<=right.length();i++){
                if(i==right.length()||String.valueOf(right.charAt(i-1)).matches("[^0-9\\.]+")){
                    if (i!=right.length())i-=1;
                    valuesArray[1]=right.substring(0,i);
                    check=true;
                    break;
                }
            }
        }
        return check;
    }
    /**
     * 对结果进行完善
     * @param expression  计算后表达式
     * @return            最简结果
     */
    private static String perfectCalculate(String prefix,String value){
        double result = Math.abs(Double.valueOf(value));
        if(StringUtils.isNotEmpty(prefix)&&prefix.indexOf("(")>0&&StringUtils.isNotEmpty(value)){
            //每层最多有一层()
            String temp = prefix.substring(0,prefix.length()-1);
            String[] values = value.split(",");
            if("max".equalsIgnoreCase(temp)){
                result = MathUtil.getMaxOfValues(values);
            }else if("min".equalsIgnoreCase(temp)){
                result = MathUtil.getMinOfValues(values);
            }
        }
        return String.valueOf(result);
    }

    /**
     * 校验表达式
     * @param expression
     * @return
     */
    private static boolean isValidExpression(String expression){
        // TODO: 2018/5/20 isValidExpression 待验证
        boolean check = false;
        if(StringUtils.isNotEmpty(expression)){
            //验证非运算符号
            if(expression.contains("`")||expression.contains("@")||expression.contains("#")||expression.contains("$")||expression.contains("_")||expression.contains("=")||expression.contains("、")||expression.contains("\\")||expression.contains("?")||expression.contains(";")||expression.contains(":")){
                check=true;
            }
            //验证()
            if(expression.contains("(")&&!check){
                expression = expression.replaceAll("\\(","\\(`");
                expression = expression.replaceAll("\\)","\\)`");
                if(expression.split("\\(").length!=expression.split("\\)").length){
                    check=true;
                }
            }
            //验证{}
            if(expression.contains("{")&&!check){
                expression = expression.replaceAll("\\{","\\{`");
                expression = expression.replaceAll("}","\\}`");
                if(expression.split("\\{").length!=expression.split("}").length){
                    check=true;
                }
            }
            //验证[]
            if(expression.contains("[")&&!check){
                expression = expression.replaceAll("\\[","\\[`");
                expression = expression.replaceAll("]","\\]`");
                if(expression.split("\\[").length!=expression.split("]").length){
                    check=true;
                }
            }
            return !check;
        }
        return check;
    }

    private static int complare(String str1, String str2) throws Exception{
        if(StringUtils.isNotEmpty(str1)&&StringUtils.isNotEmpty(str2)&&str1.contains("param")&&str2.contains("param")){
            try {
                String value1 = str1.substring("param".length());
                String value2 = str2.substring("param".length());
                if(Integer.valueOf(value1)>=Integer.valueOf(value2)){
                    return 1;
                }else{
                    return -1;
                }
            }catch (Exception e){
                throw new Exception("\n参数异常：method:complare("+str1+","+str2+") ;参数不符合规范:不为空且含有param ; Cause:"+e.getCause());
            }
        }
        throw new Exception("\n参数异常：method:complare("+str1+","+str2+") ;参数不符合规范:不为空且含有param");
    }

    private static class MathUtil {

        public static double getMaxOfValues(Double... ts){
            if(ts!=null&&ts.length>1){
                for(int i=0;i<ts.length;i++){
                    return Math.max(ts[0],getMaxOfValues(Arrays.copyOfRange(ts,1,ts.length)));
                }
            }
            return ts!=null&&ts.length>0?ts[0]:0;
        }
        public static double getMaxOfValues(String... ts){
            if(ts!=null&&ts.length>1){
                for(int i=0;i<ts.length;i++){
                    return Math.max(Double.valueOf(ts[0]),getMaxOfValues(Arrays.copyOfRange(ts,1,ts.length)));
                }
            }
            return Double.valueOf(ts!=null&&ts.length>0?ts[0]:"0");
        }

        public static double getMinOfValues(Double... ts){
            if(ts!=null&&ts.length>1){
                for(int i=0;i<ts.length;i++){
                    return Math.min(ts[0],getMinOfValues(Arrays.copyOfRange(ts,1,ts.length)));
                }
            }
            return ts!=null&&ts.length>0?ts[0]:0;
        }
        public static double getMinOfValues(String... ts){
            if(ts!=null&&ts.length>1){
                for(int i=0;i<ts.length;i++){
                    return Math.min(Double.valueOf(ts[0]),getMinOfValues(Arrays.copyOfRange(ts,1,ts.length)));
                }
            }
            return Double.valueOf(ts!=null&&ts.length>0?ts[0]:"0");
        }
    }

    public static void main(String[] args) throws Exception {
        String expression = "1+2+1.0+1+1+1+1000.10001+(100.0+10+19/20*6+5)";
        Map<String,Object> map = new HashMap<>();
        System.out.println("value:"+ MathCalculateUtil.calculate(expression,map));
    }
}
