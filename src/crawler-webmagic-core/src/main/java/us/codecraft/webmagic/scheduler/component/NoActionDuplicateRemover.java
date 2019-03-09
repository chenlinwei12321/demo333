package us.codecraft.webmagic.scheduler.component;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;

/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017-11-28 11:08:11
 */
public class NoActionDuplicateRemover implements DuplicateRemover{

    @Override
    public boolean isDuplicate(Request request, Task task) {
        return false;
    }

    @Override
    public void resetDuplicateCheck(Task task) {

    }

    @Override
    public int getTotalRequestsCount(Task task) {
        return 0;
    }
}
