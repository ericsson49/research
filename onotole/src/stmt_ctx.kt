

typealias StmtInfoCtx = StmtAnnoMap<StmtInfo>

interface StmtInfo {
  val vars: CTX
  val defsBefore: Map<String,VarDef>
  val newDefs: Map<String,VarDef>
  val phiWeb: PhiWeb?
}

data class VarDecl(val mut: Boolean, val name: String, val typ: RTType?)