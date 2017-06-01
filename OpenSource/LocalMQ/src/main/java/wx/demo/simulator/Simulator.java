package wx.demo.simulator;

import java.util.Arrays;
import java.util.List;

/**
 * Created by apple on 2017/5/23.
 */
public class Simulator {

    public static String generatorContent(int length) {

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            stringBuilder.append("A");
        }

        return stringBuilder.toString();

    }

    public static void main(String args[]) throws Exception {

        String[] strings = new String[]{"1"};

        List<String> arrayList = Arrays.asList(strings);

        System.out.println(arrayList.toArray(new String[strings.length])[0]);

    }
}
