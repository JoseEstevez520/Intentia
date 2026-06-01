package io.yourPath.dialog;

public class DialogRule {
    private String checkType;
    private String flag;
    private String nodeId;

    public DialogRule(String checkType, String flag, String nodeId) {
        this.checkType = checkType;
        this.flag = flag;
        this.nodeId = nodeId;
    }

    public String getCheckType() { return checkType; }
    public String getFlag() { return flag; }
    public String getNodeId() { return nodeId; }
}
