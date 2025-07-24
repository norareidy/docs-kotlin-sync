import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

import org.bson.Document
import org.bson.types.ObjectId
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.client.gridfs.GridFSBucket
import com.mongodb.client.gridfs.GridFSBuckets
import com.mongodb.client.gridfs.model.GridFSDownloadOptions
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts

fun main() {
    val uri = "mongodb://localhost:27017"
    
    MongoClient.create(uri).use { mongoClient ->
        // Creates a GridFS bucket on a database
        // start-createGridFSBucket
        val database = mongoClient.getDatabase("mydb")
        val gridFSBucket = GridFSBuckets.create(database)
        // end-createGridFSBucket
        
        // Creates a custom GridFS bucket named "myCustomBucket"
        // start-createCustomGridFSBucket
        val customBucket = GridFSBuckets.create(database, "myCustomBucket")
        // end-createCustomGridFSBucket
        
        // Defines options that specify configuration information for files uploaded to the bucket
        // start-uploadOptions
        val options = GridFSUploadOptions()
            .chunkSizeBytes(1048576) // 1MB chunk size
            .metadata(Document("myField", "myValue"))
        // end-uploadOptions

        // Upload a file from an input stream to the GridFS bucket
        // start-uploadFromInputStream
        val filePath = "/path/to/project.zip"
        FileInputStream(filePath).use { streamToUploadFrom ->
            // Defines options that specify configuration information for files uploaded to the bucket
            val uploadOptions = GridFSUploadOptions()
                .chunkSizeBytes(1048576)
                .metadata(Document("type", "zip archive"))

            // Uploads a file from an input stream to the GridFS bucket
            val fileId = gridFSBucket.uploadFromStream("myProject.zip", streamToUploadFrom, uploadOptions)

            // Prints the "_id" value of the uploaded file
            println("The file id of the uploaded file is: ${fileId.toHexString()}")
        }
        // end-uploadFromInputStream

        // Upload a file using an output stream
        // start-uploadFromOutputStream
        val zipFilePath = Paths.get("/path/to/project.zip")
        val data = Files.readAllBytes(zipFilePath)

        // Defines options that specify configuration information for files uploaded to the bucket
        val streamOptions = GridFSUploadOptions()
            .chunkSizeBytes(1048576)
            .metadata(Document("type", "zip archive"))

        gridFSBucket.openUploadStream("myProject.zip", streamOptions).use { uploadStream ->
            try {
                // Writes file data to the GridFS upload stream
                uploadStream.write(data)
                uploadStream.flush()

                // Prints the "_id" value of the uploaded file
                println("The file id of the uploaded file is: ${uploadStream.objectId.toHexString()}")
            } catch (e: Exception) {
                println("The file upload failed: $e")
            }
        }
        // end-uploadFromOutputStream

        // Find all files in the GridFS bucket
        // start-findAllFiles
        gridFSBucket.find().forEach { gridFSFile ->
            println(gridFSFile)
        }
        // end-findAllFiles

        // Find files matching specific criteria
        // start-findMatchingFiles
        val query = Filters.eq("metadata.type", "zip archive")
        val sort = Sorts.ascending("filename")
        
        // Retrieves 5 documents in the bucket that match the filter and prints metadata
        gridFSBucket.find(query)
            .sort(sort)
            .limit(5)
            .forEach { gridFSFile ->
                println(gridFSFile)
            }
        // end-findMatchingFiles

        // Download a file to an output stream
        // start-downloadToStream
        val downloadOptions = GridFSDownloadOptions().revision(0)

        // Downloads a file to an output stream
        FileOutputStream("/tmp/myProject.zip").use { streamToDownloadTo ->
            gridFSBucket.downloadToStream("myProject.zip", streamToDownloadTo, downloadOptions)
            streamToDownloadTo.flush()
        }
        // end-downloadToStream

        // Download a file to memory
        // start-downloadToMemory
        val fileId = ObjectId("60345d38ebfcf47030e81cc9")

        // Opens an input stream to read a file containing a specified "_id" value and downloads the file
        gridFSBucket.openDownloadStream(fileId).use { downloadStream ->
            val fileLength = downloadStream.gridFSFile.length.toInt()
            val bytesToWriteTo = ByteArray(fileLength)
            downloadStream.read(bytesToWriteTo)

            // Prints the downloaded file's contents as a string
            println(String(bytesToWriteTo, StandardCharsets.UTF_8))
        }
        // end-downloadToMemory

        // Rename a file in the GridFS bucket
        // start-renameFile
        val renameFileId = ObjectId("60345d38ebfcf47030e81cc9")
        
        // Renames the file that has a specified "_id" value to "mongodbTutorial.zip"
        gridFSBucket.rename(renameFileId, "mongodbTutorial.zip")
        // end-renameFile

        // Delete a file from the GridFS bucket
        // start-deleteFile
        val deleteFileId = ObjectId("60345d38ebfcf47030e81cc9")

        // Deletes the file that has a specified "_id" value from the GridFS bucket
        gridFSBucket.delete(deleteFileId)
        // end-deleteFile

        // Drop a GridFS bucket
        // start-dropBucket
        val dropBucket = GridFSBuckets.create(database)
        dropBucket.drop()
        // end-dropBucket
    }
}