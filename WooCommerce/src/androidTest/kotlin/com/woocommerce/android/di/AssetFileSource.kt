package com.woocommerce.android.di

import android.content.res.AssetManager
import com.github.tomakehurst.wiremock.common.BinaryFile
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.common.TextFile
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Arrays

/**
 * AssetFileSource provides a the necessary logic for WireMock to load its JSON mappings from Android assets.
 * WireMock has no Android specific behaviour so we must implement asset loading here.
 */
class AssetFileSource @JvmOverloads constructor(
    private val mAssetManager: AssetManager,
    private val mPath: String = MOCKS_PATH
) :
    FileSource {
    override fun getBinaryFileNamed(name: String): BinaryFile {
        return getBinaryFile("$mPath/$name")!!
    }

    override fun getTextFileNamed(name: String): TextFile {
        return getTextFile("$mPath/$name")!!
    }

    override fun createIfNecessary() {}
    override fun child(subDirectoryName: String): FileSource {
        return AssetFileSource(mAssetManager, "$mPath/$subDirectoryName")
    }

    override fun getPath(): String {
        return mPath
    }

    override fun getUri(): URI {
        return URI.create(mPath)
    }

    override fun listFilesRecursively(): List<TextFile> {
        val fileList: MutableList<String> = Lists.newArrayList()
        recursivelyAddFilePathsToList(mPath, fileList)
        return toTextFileList(fileList)
    }

    override fun writeTextFile(name: String, contents: String) {}
    override fun writeBinaryFile(name: String, contents: ByteArray) {}
    override fun exists(): Boolean {
        return isDirectory(mPath)
    }

    override fun deleteFile(name: String) {}
    private fun isDirectory(path: String): Boolean {
        return try {
            // Empty directories are not loaded from assets, so this works for an existence check
            // list() seems to be relatively expensive so we may wish to change this
            mAssetManager.list(path)!!.isNotEmpty()
        } catch (e: IOException) {
            false
        }
    }

    private fun recursivelyAddFilePathsToList(
        root: String,
        filePaths: MutableList<String>
    ) {
        try {
            val fileNames = Arrays.asList(
                mAssetManager.list(
                    root
                )
            )
            for (name in fileNames) {
                val path = "$root/$name"
                if (isDirectory(path)) {
                    recursivelyAddFilePathsToList(path, filePaths)
                } else {
                    filePaths.add(path)
                }
            }
        } catch (e: IOException) {
            // Ignore this
        }
    }

    private fun toTextFileList(filePaths: List<String>): List<TextFile> {
        return Lists.newArrayList(
            Iterables.transform(
                filePaths
            ) { input -> getTextFile(input!!) }
        )
    }

    private fun getBinaryFile(path: String): BinaryFile? {
        return try {
            val inputStream = mAssetManager.open(path)
            object : BinaryFile(URI.create(path)) {
                override fun getStream(): InputStream {
                    return inputStream
                }
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun getTextFile(path: String): TextFile? {
        return try {
            val inputStream = mAssetManager.open(path)
            object : TextFile(URI.create(path)) {
                override fun getStream(): InputStream {
                    return inputStream
                }

                override fun getPath(): String {
                    return path
                }
            }
        } catch (e: IOException) {
            null
        }
    }

    companion object {
        private const val MOCKS_PATH = "mocks"
    }
}
