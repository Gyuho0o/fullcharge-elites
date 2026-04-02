package com.elites.fullcharge.data

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val messagesRef: DatabaseReference = database.getReference("messages")
    private val usersRef: DatabaseReference = database.getReference("online_users")

    // 최근 50개 메시지만 유지
    private val messageLimit = 50

    // 입장 시간 (이 시간 이후의 메시지만 표시)
    private var joinedAt: Long = 0L

    fun getMessages(): Flow<List<ChatMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    child.getValue(ChatMessage::class.java)?.copy(id = child.key ?: "")
                }
                    .filter { it.timestamp >= joinedAt }  // 입장 이후 메시지만
                    .sortedBy { it.timestamp }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                // 에러 처리
            }
        }

        messagesRef.orderByChild("timestamp").limitToLast(messageLimit)
            .addValueEventListener(listener)

        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    fun setJoinedTime(time: Long) {
        joinedAt = time
    }

    fun getNewMessages(): Flow<ChatMessage> = callbackFlow {
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(ChatMessage::class.java)?.let { message ->
                    trySend(message.copy(id = snapshot.key ?: ""))
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        messagesRef.orderByChild("timestamp").limitToLast(1)
            .addChildEventListener(listener)

        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    suspend fun sendMessage(message: ChatMessage) {
        val key = messagesRef.push().key ?: UUID.randomUUID().toString()
        val messageWithId = message.copy(id = key)
        messagesRef.child(key).setValue(messageWithId.toMap()).await()
    }

    suspend fun sendSystemMessage(text: String) {
        val key = messagesRef.push().key ?: UUID.randomUUID().toString()
        val message = ChatMessage(
            id = key,
            userId = "SYSTEM",
            nickname = "시스템",
            message = text,
            timestamp = System.currentTimeMillis(),
            rank = EliteRank.NEWBIE.name,
            isSystemMessage = true
        )
        messagesRef.child(key).setValue(message.toMap()).await()
    }

    fun getOnlineUsers(): Flow<List<EliteUser>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull { child ->
                    child.getValue(EliteUser::class.java)
                }.filter { it.isOnline }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        usersRef.addValueEventListener(listener)

        awaitClose {
            usersRef.removeEventListener(listener)
        }
    }

    suspend fun joinChat(userId: String, nickname: String) {
        val user = EliteUser(
            userId = userId,
            nickname = nickname,
            sessionStartTime = System.currentTimeMillis(),
            lastActiveTime = System.currentTimeMillis(),
            isOnline = true
        )
        usersRef.child(userId).setValue(user).await()
    }

    suspend fun leaveChat(userId: String) {
        usersRef.child(userId).child("isOnline").setValue(false).await()
    }

    suspend fun updateUserActivity(userId: String) {
        usersRef.child(userId).child("lastActiveTime")
            .setValue(System.currentTimeMillis()).await()
    }

    fun getOnlineUserCount(): Flow<Int> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.children.count { child ->
                    child.getValue(EliteUser::class.java)?.isOnline == true
                }
                trySend(count)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(0)
            }
        }

        usersRef.addValueEventListener(listener)

        awaitClose {
            usersRef.removeEventListener(listener)
        }
    }
}
