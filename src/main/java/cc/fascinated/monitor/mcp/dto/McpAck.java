package cc.fascinated.monitor.mcp.dto;

/** What a write tool with nothing else to return says back, so the model can confirm it to the user. */
public record McpAck(String message) {}
