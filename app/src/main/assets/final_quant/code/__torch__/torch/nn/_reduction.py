def get_enum(reduction: str) -> int:
  _0 = "reduction=\'elementwise_mean\' is deprecated, please use reduction=\'mean\' instead."
  _1 = uninitialized(int)
  if torch.eq(reduction, "none"):
    ret = 0
  else:
    if torch.eq(reduction, "mean"):
      ret0 = 1
    else:
      _2 = torch.eq(reduction, "elementwise_mean")
      if _2:
        torch.warn(_0, 2)
        ret1 = 1
      else:
        if torch.eq(reduction, "sum"):
          ret2 = 2
        else:
          ops.prim.RaiseException("Exception")
          ret2 = _1
        ret1 = ret2
      ret0 = ret1
    ret = ret0
  return ret
def legacy_get_enum(size_average: Optional[bool],
    reduce: Optional[bool],
    emit_warning: bool=True) -> int:
  _3 = __torch__.torch.nn._reduction.legacy_get_string
  _4 = __torch__.torch.nn._reduction.get_enum
  _5 = _3(size_average, reduce, emit_warning, )
  return _4(_5, )
def legacy_get_string(size_average: Optional[bool],
    reduce: Optional[bool],
    emit_warning: bool=True) -> str:
  warning = "size_average and reduce args will be deprecated, please use reduction=\'{}\' instead."
  if torch.__is__(size_average, None):
    size_average0 = True
  else:
    size_average0 = unchecked_cast(bool, size_average)
  if torch.__is__(reduce, None):
    reduce0 = True
  else:
    reduce0 = unchecked_cast(bool, reduce)
  if size_average0:
    _6 = reduce0
  else:
    _6 = False
  if _6:
    ret = "mean"
  else:
    if reduce0:
      ret3 = "sum"
    else:
      ret3 = "none"
    ret = ret3
  if emit_warning:
    torch.warn(torch.format(warning, ret), 2)
  else:
    pass
  return ret
