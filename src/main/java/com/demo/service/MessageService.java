package com.demo.service;

import com.demo.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageService {

    int STATE_NO_AUDIT=1;
    int STATE_PASS=2;
    int STATE_REJECT=3;

    Message findById(int messageID);

    /**
     * 分页返回用户留言
     * @param pageable
     * @return
     */
     Page<Message> findByUser(String userID,Pageable pageable);

    /**
     * 添加留言
     *
     * @param message
     * @return
     */
    int create(Message message);

    /**
     * 删除留言
     *
     * @param messageID
     */
    void delById(int messageID);

    void update(Message message);

    void confirmMessage(int messageID);

    void rejectMessage(int messageID);

    Page<Message> findWaitState(Pageable pageable);

    Page<Message> findPassState(Pageable pageable);

}
