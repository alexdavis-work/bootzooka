package com.softwaremill.bootzooka.rest

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.softwaremill.bootzooka.BootzookaServletSpec
import com.softwaremill.bootzooka.service.data.UserJson
import com.softwaremill.bootzooka.service.user.UserService
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatra.auth.Scentry

class UsersServletWithAuthSpec extends BootzookaServletSpec {

  def onServletWithMocks(authenticated: Boolean, testToExecute: (UserService, Scentry[UserJson]) => Unit) {
    val userService = mock[UserService]

    val mockedScentry = mock[Scentry[UserJson]]
    when(mockedScentry.isAuthenticated(any[HttpServletRequest],any[HttpServletResponse])) thenReturn authenticated

    val servlet: MockUsersServlet = new MockUsersServlet(userService, mockedScentry)
    addServlet(servlet, "/*")

    testToExecute(userService, mockedScentry)
  }

  "GET /logout" should "call logout() when user is already authenticated" in {
    onServletWithMocks(authenticated = true, testToExecute = (userService, mock) =>
      get("/logout") {
        verify(mock, times(2)).isAuthenticated(any[HttpServletRequest],any[HttpServletResponse]) // before() and get('/logout')
        verify(mock).logout()(any[HttpServletRequest],any[HttpServletResponse])
        verifyZeroInteractions(userService)
      }
    )
  }

  "GET /logout" should "not call logout() when user is not authenticated" in {
    onServletWithMocks(authenticated = false, testToExecute = (userService, mock) =>
      get("/logout") {
        verify(mock, times(2)).isAuthenticated(any[HttpServletRequest], any(classOf[HttpServletResponse])) // before() and get('/logout')
        verify(mock, never).logout()
        verifyZeroInteractions(userService)
      }
    )
  }

  "GET /" should "return user information" in {
    onServletWithMocks(authenticated = true, testToExecute = (userService, mock) =>
      get("/") {
        status should be (200)
        body should be ("{\"id\":\"" + "1" * 24  + "\",\"login\":\"Jas Kowalski\",\"email\":\"kowalski@kowalski.net\",\"token\":\"token\"}")
      }
    )
  }

  class MockUsersServlet(userService: UserService, mockedScentry: Scentry[UserJson]) extends UsersServlet(userService) with MockitoSugar {
    override def scentry(implicit request: javax.servlet.http.HttpServletRequest) = mockedScentry
    override def user(implicit request: javax.servlet.http.HttpServletRequest) = new UserJson("1" * 24, "Jas Kowalski", "kowalski@kowalski.net", "token")
  }
}

