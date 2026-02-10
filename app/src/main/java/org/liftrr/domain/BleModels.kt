package org.liftrr.domain

import org.json.JSONObject
import java.util.*

interface LiftrrCommand {
    val name: String
    val body: JSONObject
    
    fun toJson(phoneEpochMs: Long = System.currentTimeMillis()): String {
        // Sync time into the body object directly
        if (!body.has("phoneEpochMs")) {
            body.put("phoneEpochMs", phoneEpochMs)
        }
        
        // Construct the final envelope
        val envelope = JSONObject()
        envelope.put("cmd", name)
        envelope.put("body", body)
        return envelope.toString()
    }
}

class PingCommand : LiftrrCommand {
    override val name = "ping"
    override val body = JSONObject()
}

class CapabilitiesGetCommand : LiftrrCommand {
    override val name = "capabilities.get"
    override val body = JSONObject()
}

class TimeSyncCommand(phoneEpochMs: Long) : LiftrrCommand {
    override val name = "time.sync"
    override val body = JSONObject().apply {
        put("phoneEpochMs", phoneEpochMs)
    }
}

class ModeSetCommand(mode: String) : LiftrrCommand {
    override val name = "mode.set"
    override val body = JSONObject().apply {
        put("mode", mode)
    }
}

class SessionStartCommand(lift: String, phoneEpochMs: Long? = null) : LiftrrCommand {
    override val name = "session.start"
    override val body = JSONObject().apply {
        put("lift", lift)
        if (phoneEpochMs != null) {
            put("phoneEpochMs", phoneEpochMs)
        }
    }
}

class SessionEndCommand : LiftrrCommand {
    override val name = "session.end"
    override val body = JSONObject()
}

class SessionsListCommand(cursor: Long, limit: Long) : LiftrrCommand {
    override val name = "sessions.list"
    override val body = JSONObject().apply {
        put("cursor", cursor)
        put("limit", limit)
    }
}

class SessionsClearCommand : LiftrrCommand {
    override val name = "sessions.clear"
    override val body = JSONObject()
}

class SessionStreamCommand(sessionId: String) : LiftrrCommand {
    override val name = "session.stream"
    override val body = JSONObject().apply {
        put("sessionId", sessionId)
    }
}

data class SessionItem(
    val fileName: String,
    val size: Long = 0,
    val mtime: Long = 0
) {
    val liftName: String
        get() = fileName.split("-").getOrNull(4) ?: "Unknown"
}

object LiftrrUuids {
    val SERVICE = UUID.fromString("c7ccb0e4-7cd7-45f6-9693-b3dda2d77672")
    val COMMAND = UUID.fromString("b0f2a8fb-9a63-4d9f-8ffc-80501fda2359")
    val STATUS = UUID.fromString("27746aa3-5fae-44c1-a1eb-65844cc315dc")
    val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
