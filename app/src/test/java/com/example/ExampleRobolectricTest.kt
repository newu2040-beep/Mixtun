package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.FilePreviewEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mixtun", appName)
  }

  @Test
  fun `test json tree parsing with android json classes`() = runBlocking {
    val sampleJson = """{"user": {"name": "Rahul", "active": true, "count": 42}}"""
    val result = FilePreviewEngine.parseJsonTree(sampleJson)
    assertTrue(result.isSuccess)
    val root = result.getOrNull()
    assertNotNull(root)
    assertEquals(1, root!!.children.size)
    assertEquals("user", root.children[0].key)
  }
}
