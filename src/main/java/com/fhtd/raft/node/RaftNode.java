package com.fhtd.raft.node;

import com.fhtd.raft.role.Learner;
import com.fhtd.raft.role.Role;
import com.fhtd.raft.role.RoleType;

/**
 * @author liuqi19
 * @version : RaftNode, 2019-05-24 14:30 liuqi19
 */
public class RaftNode extends Node {

    /**
     * 此节点commit的index
     */
    private long match = -1;

    private long next;

    private boolean active = false;

    protected RoleType type;


    public RaftNode(Node node) {
        this(node,false);

    }

    public RaftNode(Node node,boolean becomeLearner){
        super(node);
        this.next = match + 1;
        if(becomeLearner) this.becomeLearner();
    }

    public long next() {
        return next;
    }

    public long match() {
        return match;
    }


    /**
     * 更改index,当成功同步日志之后，进行update,next 有可能大于远超于match
     *
     * @param index
     * @return
     */
    public boolean update(long index) {
        boolean updated = false;
        if (this.match < index) {
            this.match = index;
            updated = true;
        }

        if (this.next < index + 1)
            this.next = index + 1;
        return updated;
    }


    /**
     * 减少
     *
     * @param rejected 被follower拒绝的index
     * @param confirmIndex follower已经确认append的index
     * @return
     */
    public boolean decrease(long rejected, long confirmIndex) {
        if (rejected < this.match) return false;

        if (this.next - 1 != rejected) return false;

        this.next = Math.min(rejected, confirmIndex + 1);

        if (this.next == rejected) this.next = this.match + 1;

        if (this.next <= this.match)
            this.match = this.next - 1;

        return true;
    }

    public void optimisticUpdate(long index) {
        this.next = index + 1;
    }


    public void active(boolean value) {
        if (this.active == value) return;

        if (value) {
            this.reset();
        }
        this.active = value;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * 变为复制者
     */
    public void becomeProbe() {

        this.next = this.match + 1;

    }

    public void becomeLearner(){
        this.type = RoleType.LEARNER;
    }

    public void becomeFollower(){
    }

    public boolean is(RoleType type) {
        return type == this.type;
    }

    protected void reset() {
        this.next = this.match + 1;
    }
}
