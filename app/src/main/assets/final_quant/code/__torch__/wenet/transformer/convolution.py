class ConvolutionModule(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  lorder : int
  use_layer_norm : bool
  pointwise_conv1 : __torch__.torch.nn.modules.conv.Conv1d
  depthwise_conv : __torch__.torch.nn.modules.conv.___torch_mangle_7.Conv1d
  norm : __torch__.torch.nn.modules.normalization.___torch_mangle_8.LayerNorm
  pointwise_conv2 : __torch__.torch.nn.modules.conv.___torch_mangle_9.Conv1d
  activation : __torch__.wenet.transformer.swish.Swish
  def forward(self: __torch__.wenet.transformer.convolution.ConvolutionModule,
    x: Tensor,
    mask_pad: Optional[Tensor]=None,
    cache: Optional[Tensor]=None) -> Tuple[Tensor, Tensor]:
    x0 = torch.transpose(x, 1, 2)
    if torch.__isnot__(mask_pad, None):
      mask_pad1 = unchecked_cast(Tensor, mask_pad)
      _0 = torch.masked_fill_(x0, torch.bitwise_not(mask_pad1), 0.)
      mask_pad0 = mask_pad1
    else:
      mask_pad0 = mask_pad
    if torch.gt(self.lorder, 0):
      if torch.__is__(cache, None):
        x3 = __torch__.torch.nn.functional._pad(x0, [self.lorder, 0], "constant", 0., )
        x2 = x3
      else:
        cache0 = unchecked_cast(Tensor, cache)
        _1 = torch.eq(torch.size(cache0, 0), torch.size(x0, 0))
        if _1:
          pass
        else:
          ops.prim.RaiseException("Exception")
        _2 = torch.eq(torch.size(cache0, 1), torch.size(x0, 1))
        if _2:
          pass
        else:
          ops.prim.RaiseException("Exception")
        x2 = torch.cat([cache0, x0], 2)
      _3 = torch.gt(torch.size(x2, 2), self.lorder)
      if _3:
        pass
      else:
        ops.prim.RaiseException("Exception")
      _4 = torch.slice(x2, 0, 0, 9223372036854775807, 1)
      _5 = torch.slice(_4, 1, 0, 9223372036854775807, 1)
      new_cache0 = torch.slice(_5, 2, torch.neg(self.lorder), 9223372036854775807, 1)
      x1, new_cache = x2, new_cache0
    else:
      new_cache1 = torch.tensor([0.], dtype=ops.prim.dtype(x0), device=ops.prim.device(x0), requires_grad=False)
      x1, new_cache = x0, new_cache1
    x4 = (self.pointwise_conv1).forward(x1, )
    x5 = __torch__.torch.nn.functional.glu(x4, 1, )
    x6 = (self.depthwise_conv).forward(x5, )
    if self.use_layer_norm:
      x7 = torch.transpose(x6, 1, 2)
    else:
      x7 = x6
    x8 = (self.activation).forward((self.norm).forward(x7, ), )
    if self.use_layer_norm:
      x9 = torch.transpose(x8, 1, 2)
    else:
      x9 = x8
    x10 = (self.pointwise_conv2).forward(x9, )
    if torch.__isnot__(mask_pad0, None):
      mask_pad2 = unchecked_cast(Tensor, mask_pad0)
      _6 = torch.masked_fill_(x10, torch.bitwise_not(mask_pad2), 0.)
    else:
      pass
    _7 = (torch.transpose(x10, 1, 2), new_cache)
    return _7
