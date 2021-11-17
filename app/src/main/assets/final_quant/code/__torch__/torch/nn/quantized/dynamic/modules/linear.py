class Linear(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  in_features : int
  out_features : int
  scale : float
  zero_point : int
  version : int
  _packed_params : __torch__.torch.nn.quantized.modules.linear.LinearPackedParams
  def forward(self: __torch__.torch.nn.quantized.dynamic.modules.linear.Linear,
    x: Tensor) -> Tensor:
    _0 = uninitialized(Tensor)
    _1 = torch.eq(self._packed_params.dtype, 12)
    if _1:
      if torch.lt(self.version, 4):
        _2 = self._packed_params._packed_params
        Y1 = ops.quantized.linear_dynamic(x, _2, False)
        Y0 = Y1
      else:
        _3 = self._packed_params._packed_params
        Y2 = ops.quantized.linear_dynamic(x, _3, True)
        Y0 = Y2
      Y = Y0
    else:
      _4 = torch.eq(self._packed_params.dtype, 5)
      if _4:
        _5 = self._packed_params._packed_params
        Y4 = ops.quantized.linear_dynamic_fp16(x, _5)
        Y3 = Y4
      else:
        ops.prim.RaiseException("Exception")
        Y3 = _0
      Y = Y3
    _6 = torch.to(Y, ops.prim.dtype(x), False, False, None)
    return _6
