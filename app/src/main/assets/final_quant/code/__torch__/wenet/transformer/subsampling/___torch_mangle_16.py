class Conv2dSubsampling4(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  right_context : int
  subsampling_rate : int
  conv : __torch__.torch.nn.modules.container.Sequential
  out : __torch__.torch.nn.modules.container.___torch_mangle_15.Sequential
  pos_enc : __torch__.wenet.transformer.embedding.RelPositionalEncoding
  def forward(self: __torch__.wenet.transformer.subsampling.___torch_mangle_16.Conv2dSubsampling4,
    x: Tensor,
    x_mask: Tensor,
    offset: int=0) -> Tuple[Tensor, Tensor, Tensor]:
    x0 = torch.unsqueeze(x, 1)
    x1 = (self.conv).forward(x0, )
    b, c, t, f, = torch.size(x1)
    _0 = self.out
    _1 = torch.contiguous(torch.transpose(x1, 1, 2), memory_format=0)
    _2 = torch.view(_1, [b, t, torch.mul(c, f)])
    x2 = (_0).forward(_2, )
    _3 = (self.pos_enc).forward(x2, offset, )
    x3, pos_emb, = _3
    _4 = torch.slice(x_mask, 0, 0, 9223372036854775807, 1)
    _5 = torch.slice(_4, 1, 0, 9223372036854775807, 1)
    _6 = torch.slice(torch.slice(_5, 2, 0, -2, 2), 0, 0, 9223372036854775807, 1)
    _7 = torch.slice(_6, 1, 0, 9223372036854775807, 1)
    _8 = (x3, pos_emb, torch.slice(_7, 2, 0, -2, 2))
    return _8
  def position_encoding(self: __torch__.wenet.transformer.subsampling.___torch_mangle_16.Conv2dSubsampling4,
    offset: int,
    size: int) -> Tensor:
    _9 = (self.pos_enc).position_encoding(offset, size, )
    return _9
