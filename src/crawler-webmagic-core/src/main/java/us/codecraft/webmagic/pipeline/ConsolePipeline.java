package us.codecraft.webmagic.pipeline;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Write results in console.<br>
 * Usually used in test.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 */
public class ConsolePipeline implements Pipeline {

    @Override
    public void process(ResultItems resultItems, Task task) {
        System.out.println("get page: " + resultItems.getRequest().getUrl());
//        for (Map.Entry<String, Object> entry : resultItems.getAll().entrySet()) {
//        	Object v = entry.getValue();
//        	if(List.class.isAssignableFrom(v.getClass())){
//                System.out.println(entry.getKey() + ":\t" +Arrays.toString(((List)v).toArray()));
//        	}else{
//                System.out.println(entry.getKey() + ":\t" + entry.getValue());
//        	}
//        }
    }
}
