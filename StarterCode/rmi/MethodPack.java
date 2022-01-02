package rmi;

import java.io.Serializable;
import java.util.Arrays;

public class MethodPack implements Serializable {
    private String methodName;
    private Object[] paras;

    public MethodPack(String methodName, Object[] paras) {
        this.methodName = methodName;
        this.paras = paras;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getParas() {
        return paras;
    }

    public void setParas(Object[] paras) {
        this.paras = paras;
    }

    @Override
    public String toString() {
        return "MethodPack{" +
                ", methodName='" + methodName + '\'' +
                ", paras=" + Arrays.toString(paras) +
                '}';
    }
}
