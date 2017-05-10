package com.catikkas

package object aiorserver {

  /**
   * Convenient syntax to circumvent wartremover:NonUnitStatements.
   * Turns out you can do
   * <pre>val _ = x</pre>
   * only once within a scope. This can get annoying in certain situations and you might not
   * want to plainly ignore with `@SuppressWarnings` annotation. Instead you can use the
   * `discarding` syntax to make it explicit that you are discarding the return value of the
   * given statement.
   *
   * Example:
   * <pre>
   * val robot  = context.actorOf(Robot.props, "robot")
   * val server = context.actorOf(Server.props(robot), "server")
   * discarding { context watch robot }
   * discarding { context watch server }
   * context become initialized(robot, server)
   * </pre>
   *
   * @param op
   */
  def discarding(op: => Any): Unit = {
    val _ = op
    ()
  }

}
