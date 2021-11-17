class RelPositionalEncoding(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  d_model : int
  xscale : float
  max_len : int
  pe : Tensor
  dropout : __torch__.torch.nn.modules.dropout.Dropout
  def forward(self: __torch__.wenet.transformer.embedding.RelPositionalEncoding,
    x: Tensor,
    offset: int=0) -> Tuple[Tensor, Tensor]:
    _0 = torch.lt(torch.add(offset, torch.size(x, 1)), self.max_len)
    if _0:
      pass
    else:
      ops.prim.RaiseException("Exception")
    _1 = torch.to(self.pe, ops.prim.device(x), None, False, False)
    self.pe = _1
    x0 = torch.mul(x, self.xscale)
    _2 = torch.slice(self.pe, 0, 0, 9223372036854775807, 1)
    _3 = torch.add(offset, torch.size(x0, 1))
    pos_emb = torch.slice(_2, 1, offset, _3, 1)
    _4 = ((self.dropout).forward(x0, ), (self.dropout).forward(pos_emb, ))
    return _4
  def position_encoding(self: __torch__.wenet.transformer.embedding.RelPositionalEncoding,
    offset: int,
    size: int) -> Tensor:
    _5 = torch.lt(torch.add(offset, size), self.max_len)
    if _5:
      pass
    else:
      ops.prim.RaiseException("Exception")
    _6 = self.dropout
    _7 = torch.slice(self.pe, 0, 0, 9223372036854775807, 1)
    _8 = torch.slice(_7, 1, offset, torch.add(offset, size), 1)
    return (_6).forward(_8, )
class PositionalEncoding(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  d_model : int
  xscale : float
  max_len : int
  pe : Tensor
  dropout : __torch__.torch.nn.modules.dropout.Dropout
  def forward(self: __torch__.wenet.transformer.embedding.PositionalEncoding,
    x: Tensor,
    offset: int=0) -> Tuple[Tensor, Tensor]:
    _9 = torch.lt(torch.add(offset, torch.size(x, 1)), self.max_len)
    if _9:
      pass
    else:
      ops.prim.RaiseException("Exception")
    _10 = torch.to(self.pe, ops.prim.device(x), None, False, False)
    self.pe = _10
    _11 = torch.slice(self.pe, 0, 0, 9223372036854775807, 1)
    pos_emb = torch.slice(_11, 1, offset, torch.add(offset, torch.size(x, 1)), 1)
    x1 = torch.add(torch.mul(x, self.xscale), pos_emb, alpha=1)
    _12 = ((self.dropout).forward(x1, ), (self.dropout).forward(pos_emb, ))
    return _12
