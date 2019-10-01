package com.shadowtesseract.politests.database.engine

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.shadowtesseract.politests.database.model.*
import com.shadowtesseract.politests.logger.Level
import com.shadowtesseract.politests.logger.Logger.logErr
import com.shadowtesseract.politests.logger.Logger.logMsg
import java.util.*


/**
 * This class will be singleton
 * Main function of this class will be sending requests of other classes to Firebase and passing results back
 */

fun statPath(uid: String) = "users/$uid/data/stats"
private const val TAG = "FC"
object FirebaseConnector : Observable
{

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    var listOfObservers = mutableListOf<Observer>()

    data class ListOfQuestions(val questionList: MutableList<Question> = mutableListOf())

    fun downloadQuestionBase(request:QuestionRequest)
    {

        logMsg("FC","Uruchamianie pobierania bazy")
        val ref : DatabaseReference = database.reference.ref.child(request.getAsPath())
        logMsg("FC",ref.toString())
        val baseListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                logMsg("FC", "Odebrano dane: "+dataSnapshot)
                var data : String = dataSnapshot.value.toString();
                logMsg("FC", "Data: "+data)

                val g = Gson()
                val listType = object : TypeToken<ArrayList<Question>>() {}.type
                val qdb = g.fromJson<MutableList<Question>>(data,  listType)

                logMsg("FC", qdb.toString())

                notify(QuestionBase(qdb))
            }

            override fun onCancelled(p0: DatabaseError) {
                logMsg("ERR", p0.message)
            }
        }

        ref.addListenerForSingleValueEvent(baseListener)
    }


    fun downloadPathIndexOfQuestionBase(request:QuestionRequest)
    {
        logMsg("FC","Uruchamianie pobierania indexu patha")
        logMsg("FC", request.toString())
        val ref : DatabaseReference = database.reference.ref.child(request.getAsIndex())
        logMsg("FC",ref.toString())

        val indexListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                logMsg("FC", "Odebrano dane: "+dataSnapshot)
                var data : String = dataSnapshot.value.toString();
                logMsg("FC", "Data: "+data)

                if (data == "null")
                    return

                val jsonParser = JsonParser()
                val jsonObject = jsonParser.parse(data).asJsonObject
                val pathIndex = PathIndex(jsonObject.getAsJsonArray("names").toMutableList().map
                { it -> it.toString() }.toMutableList().map { it -> it.replace("\"", "")}.toMutableList() ,
                        jsonObject.getAsJsonArray("hashes").toMutableList().map
                        { it -> it.toString() }.toMutableList().map { it -> it.replace("\"", "")}.toMutableList())


                logMsg("FC", "Sparsowano dane: "+pathIndex)
                notify(pathIndex)
            }

            override fun onCancelled(p0: DatabaseError) {
                logMsg("ERR", p0.message)
            }
        }
        ref.addListenerForSingleValueEvent(indexListener)
    }

    fun downloadTextIndexOfQuestionBase(request:QuestionRequest)
    {
        logMsg("FC","Uruchamianie pobierania indexu bazy")
        logMsg("FC", request.toString())
        val ref : DatabaseReference = database.reference.ref.child(request.getAsTestIndex())
        logMsg("FC",ref.toString())

        val indexListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                logMsg("FC", "Odebrano dane: "+dataSnapshot)
                var data : String = dataSnapshot.value.toString();
                logMsg("FC", "Odebrano dane: "+data)
                if(data=="null")
                {
                    var list : MutableList<IndexInstance> = mutableListOf()
                    notify(list)
                }
                else
                {
                    val listType = object : TypeToken<ArrayList<IndexInstance>>() {}.type
                    var list: MutableList<IndexInstance> = Gson().fromJson(data, listType)
                    logMsg("FC", "List: " + list.toString())
                    for (e in list)
                        logMsg("FC", "V: " + e.toString())
                    logMsg(TAG, "Pobrano indeks testu")
                    notify(list)
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                logMsg("FC", p0.message)
                var list : MutableList<IndexInstance> = mutableListOf()
                notify(list)
            }
        }
        ref.addListenerForSingleValueEvent(indexListener)
    }

    fun uploadUserStatistics(statistics: UserStatistics) {
        val stats = statistics.serialize()
        val path = statPath(auth.currentUser!!.uid)
        val ref = database.getReference(path)
        ref.setValue(stats)
    }

    fun downloadUserStatistics(observer: Observer) {
        val path = statPath(auth.currentUser!!.uid)
        val ref = database.getReference(path)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                logErr(TAG, "Nie udało się pobrać statystyk", Level.HIGH)
                observer.run(false)
            }

            override fun onDataChange(p0: DataSnapshot)
            {
                if (p0.value != null)
                {
                    logMsg(TAG, "Pobrano statystyki użytkownika")
                    val str = p0.value.toString()
                    val stats = UserStatistics.deserialize(str)
                    observer.run(stats)
                }
                else
                {
                    logErr(TAG, "Nie udało się pobrac statystyk użytkownika: ${auth.currentUser!!.uid}", Level.HIGH)
                }

            }
        })
    }

    override fun notify(obj: Any) {
        for(o : Observer in listOfObservers)
            o.run(obj)
    }

    override fun remove(observer: Observer) {
        listOfObservers.remove(observer)
    }

    override fun add(observer: Observer) {
        listOfObservers.add(observer)
    }

}