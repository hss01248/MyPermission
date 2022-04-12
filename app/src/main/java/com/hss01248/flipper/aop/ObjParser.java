package com.hss01248.flipper.aop;

import android.content.Intent;
import android.os.Message;
import android.text.TextUtils;
import android.util.Pair;



import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Kale
 * @date 2016/3/23
 */
public class ObjParser {


    // 基本数据类型
    private final static String[] TYPES = {"int", "java.lang.String", "boolean", "char",
            "float", "double", "long", "short", "byte",
            "java.lang.Integer", "java.lang.Boolean",
            "java.lang.Float", "java.lang.Double",
            "java.lang.Char", "java.lang.Short",
            "java.lang.Byte"};
    public static final String NEWLINE_AND_SPACE = "  ";//\n
    public static String parseObj(Object object) {
        return parseObj(object, false, true);
    }

    public static String parseObj(Object object, boolean printMethod, boolean ignoreToStringImpl) {
        if (object == null) {
            return "null";
        }

         String simpleName = object.getClass().getSimpleName()  + " @ " + Integer.toHexString(object.hashCode());
        if (object.getClass().isArray()) {
            StringBuilder msg = new StringBuilder("Temporarily not support more than two dimensional Array!");
            int dim = ArrayParser.getArrayDimension(object);
            simpleName = Integer.toHexString(object.hashCode()) +" @ "+object.getClass().getSimpleName();
            switch (dim) {
                case 1:
                    Pair pair = ArrayParser.arrayToString(object);
                    msg = new StringBuilder(simpleName.replace("[]", "[" + pair.first + "] {\n  "));
                    msg.append(pair.second).append(NEWLINE_AND_SPACE);
                    break;
                case 2:
                    Pair pair1 = ArrayParser.arrayToObject(object);
                    Pair pair2 = (Pair) pair1.first;
                    msg = new StringBuilder(simpleName.replace("[][]", "[" + pair2.first + "][" + pair2.second + "] {\n  "));
                    msg.append(pair1.second).append(NEWLINE_AND_SPACE);
                    break;
                default:
                    break;
            }
            return msg + "}";
        } else if (object instanceof Collection) {
            Collection collection = (Collection) object;
            //String msg = "%s size = %d [\n";
            StringBuilder sb = new StringBuilder();
            sb.append(simpleName)
                    .append(" size = ")
                    .append(collection.size())
                    .append(" [\n");
            //msg = String.format(Locale.ENGLISH, msg, simpleName, collection.size());
            if (!collection.isEmpty()) {
                Iterator iterator = collection.iterator();
                int flag = 0;
                while (iterator.hasNext()) {
                    //String itemString = "[%d]:%s%s\n";
                    Object item = iterator.next();
                    sb.append("[")
                            .append(flag)
                            .append("]:")
                            .append(objectToString(item, printMethod, ignoreToStringImpl))
                            .append(flag++ < collection.size() - 1 ? ",\n" : "");

                   /* msg += String.format(Locale.ENGLISH, itemString,
                            flag,
                            objectToString(item,printMethod,ignoreToStringImpl),
                            flag++ < collection.size() - 1 ? ",\n  " : "  ");*/
                }
            }
            return sb.append("]").toString();
        } else if (object instanceof Map) {
            StringBuilder sb = new StringBuilder();
            sb.append(simpleName)
                    .append(" {\n");
            //String msg = simpleName + " {\n";
            Map map = (Map) object;
            Iterator<Map.Entry> iterator = map.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry next = iterator.next();
                Object key = next.getKey();
                Object val = next.getValue();
                sb
                        .append(objectToString(key, printMethod, ignoreToStringImpl))
                        .append(" -> ")
                        .append(objectToString(val, printMethod, ignoreToStringImpl));
                if(iterator.hasNext()){
                    sb.append("\n");
                }
            }
            return sb.append("}").toString();
        } else {
            return objectToString(object, printMethod, ignoreToStringImpl);
        }
    }


    protected static <T> String objectToString(T object) {
        return objectToString(object, false, true);
    }

    /**
     * 将对象转化为String
     */
    protected static String objectToString(Object object, boolean printMethod, boolean ignoreToStringImpl) {
        if (object == null) {
            return "Object{object is null}";
        }
        /*if(object instanceof Reference){
            Reference reference = (Reference) object;
           Object object2 = reference.get();
            if (object2 == null) {
                return reference.toString()+"{real object is null}";
            }
            object = object2;
        }*/
        String toStr = object.toString();


        String type0 = object.getClass().getName();
        boolean isSimpleType = false;
        for (String type : TYPES) {
            if (type.equals(type0)) {
                isSimpleType = true;
                break;
            }
        }
        if (isSimpleType) {
            return object.toString();
        }

        //String text = ReferenceParse.parseObj(object);
        String text = "";
        if (!TextUtils.isEmpty(text)) {
            return text;
        }


        //忽略类本身的tostring实现,强制打印所有属性和方法
        if (!toStr.startsWith(object.getClass().getName() + "@")) {
            if (!ignoreToStringImpl) {
                return toStr;
            }
        }

        StringBuilder builder = new StringBuilder(toStr + "{");
        builder.append(NEWLINE_AND_SPACE);
        Field[] fields = object.getClass().getDeclaredFields();
        List<Field> fieldList = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            //不打印静态且final的属性:
            if (Modifier.isStatic(field.getModifiers())
                    && Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            fieldList.add(field);
        }
        int size = fieldList.size();


        for (int i = 0; i < size; i++) {
            Field field = fieldList.get(i);
            boolean flag = false;
            for (String type : TYPES) {
                if (field.getType().getName().equalsIgnoreCase(type)) {
                    flag = true;
                    Object value = null;
                    try {
                        value = field.get(object);
                    } catch (IllegalAccessException e) {
                        value = e;
                    } finally {
                        //规则1:只打印一层,内部不再解析,而是用toString
                        //规则2: null值不打印
                        if (value != null) {
                            builder.append(field.getName())
                                    .append(" = ")
                                    .append(value.toString());
                            appendIfFieldNotEnd(i, fieldList, builder);
                        }
                    }
                }
            }
            if (!flag) {
                try {
                    Object objectf = field.get(object);
                    //规则同上,但如果是一些特殊的类,可以再次打印
                    if (objectf != null) {
                        String objStr = formatInnerObj(objectf);
                        builder.append(field.getName())
                                .append(" = ")
                                .append(objStr);
                        appendIfFieldNotEnd(i, fieldList, builder);
                    } else {
                        //不打印null的key 和value
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                    builder.append(field.getName())
                            .append(" = ")
                            .append("object(parse err: ")
                            .append(e.getMessage()).append(")");
                    appendIfFieldNotEnd(i, fieldList, builder);
                }
            }
        }
        //builder.delete(builder.length()-2,builder.length()-1);
        builder.append("\n}");

        if (printMethod) {
            builder.append("\n\n methods:\n");
            Method[] methods = object.getClass().getDeclaredMethods();
            for (Method method : methods) {
                method.setAccessible(true);
                String name = method.getName();
                if (name.contains("$")) {
                    continue;
                }

                Class[] clazz = method.getParameterTypes();
                StringBuilder stringBuilder = new StringBuilder();
                if (clazz != null && clazz.length > 0) {
                    for (int i = 0; i < clazz.length; i++) {
                        stringBuilder.append(clazz[i].getSimpleName());
                        if (i != clazz.length - 1) {
                            stringBuilder.append(", ");
                        }
                    }
                }
                String params = stringBuilder.toString();

                builder
                        .append("\nanotations:")
                        .append(Arrays.toString(method.getAnnotations()))
                        .append("\n")
                        .append(method.getReturnType())
                        .append(" ")
                        .append(method.getName())
                        .append("(")
                        .append(params)
                        .append(")")
                        .append("\n");
            }
        }
       /* int idx = builder.lastIndexOf(",");
        if(idx > 0){
            return builder.replace(idx, idx+2, "  }").toString();
        }*/
        return builder.toString();
    }

    private static void appendIfFieldNotEnd(int i, List<Field> fields, StringBuilder builder) {
        if (i == fields.size() - 1) {
            return;
        }
        builder.append(",\n")
                //.append(String.format("%s = %s,\n", field.getName(), objStr))
                .append(NEWLINE_AND_SPACE);//"Object"
    }

    private static String formatInnerObj(Object objectf) {
        if (objectf == null) {
            return null;
        }

        if (objectf.getClass().isArray()) {
            try {
                return Arrays.toString((Object[]) objectf);
            } catch (Throwable e) {
                if (objectf instanceof float[]) {
                    return Arrays.toString((float[]) objectf);
                } else if (objectf instanceof int[]) {
                    return Arrays.toString((int[]) objectf);
                } else if (objectf instanceof long[]) {
                    return Arrays.toString((long[]) objectf);
                } else if (objectf instanceof double[]) {
                    return Arrays.toString((double[]) objectf);
                } else if (objectf instanceof boolean[]) {
                    return Arrays.toString((boolean[]) objectf);
                } else if (objectf instanceof char[]) {
                    return Arrays.toString((char[]) objectf);
                } else if (objectf instanceof byte[]) {
                    return Arrays.toString((byte[]) objectf);
                } else {
                    return objectf + "";
                }


            }

        }
        return objectf.toString();
    }
}
