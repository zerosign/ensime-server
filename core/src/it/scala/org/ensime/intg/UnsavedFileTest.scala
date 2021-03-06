// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/gpl-3.0.en.html
package org.ensime.intg

import org.ensime.api._
import org.ensime.fixture._
import org.ensime.util.EnsimeSpec
import org.ensime.util.file._

/**
 * Verifies common operations work correctly for unsaved files.
 */
class UnsavedFileTest
    extends EnsimeSpec
    with IsolatedProjectFixture
    with IsolatedEnsimeConfigFixture
    with IsolatedTestKitFixture {

  val original = EnsimeConfigFixture.TimingTestProject

  "ensime-server" should "handle unsaved files" in {
    withEnsimeConfig { implicit config =>
      withTestKit { implicit testkit =>
        withProject { (project, asyncHelper) =>
          import testkit._

          val sourceRoot = scalaMain(config)
          val missing    = sourceRoot / "p1/Missing.scala"

          assert(!missing.exists)

          val inMemory = SourceFileInfo(
            RawFile(missing.toPath),
            Some("class Foo { def main = { System.out.println(1) } }"),
            None
          )

          project ! TypecheckFileReq(inMemory)
          expectMsg(VoidResponse)

          project ! SymbolDesignationsReq(Right(inMemory),
                                          0,
                                          50,
                                          SourceSymbol.allSymbols)
          expectMsgPF() {
            case SymbolDesignations(inMemory.file,
                                    syms: List[SymbolDesignation])
                if syms.nonEmpty =>
          }

          project ! CompletionsReq(inMemory, 27, 0, false, false)
          expectMsgPF() {
            case CompletionInfoList("Sy", candidates)
                if candidates.exists(_.name == "System") =>
          }
        }
      }
    }
  }

  it should "handle unsaved empty files" in {
    withEnsimeConfig { implicit config =>
      withTestKit { implicit testkit =>
        withProject { (project, asyncHelper) =>
          import testkit._

          val sourceRoot   = scalaMain(config)
          val unsavedEmpty = sourceRoot / "p1/UnsavedEmpty.scala"

          assert(!unsavedEmpty.exists)

          val unsaved = SourceFileInfo(RawFile(unsavedEmpty.toPath), None, None)
          project ! TypecheckFileReq(unsaved)
          expectMsgPF() { case EnsimeServerError(e) => }

          project ! SymbolDesignationsReq(Right(unsaved),
                                          0,
                                          0,
                                          SourceSymbol.allSymbols)
          expectMsgPF() { case EnsimeServerError(e) => }

          project ! CompletionsReq(unsaved, 0, 0, false, false)
          expectMsgPF() { case EnsimeServerError(e) => }

          project ! FqnOfSymbolAtPointReq(
            SourceFileInfo(EnsimeFile(unsavedEmpty), None, None),
            0
          )
          expectMsgPF() { case EnsimeServerError(e) => }

        }
      }
    }
  }

}
